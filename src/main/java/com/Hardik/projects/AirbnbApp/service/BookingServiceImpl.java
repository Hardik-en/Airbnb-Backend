package com.Hardik.projects.AirbnbApp.service;


import com.Hardik.projects.AirbnbApp.dto.*;
import com.Hardik.projects.AirbnbApp.entity.*;
import com.Hardik.projects.AirbnbApp.entity.enums.BookingStatus;
import com.Hardik.projects.AirbnbApp.exception.ResourceNotFoundException;
import com.Hardik.projects.AirbnbApp.exception.UnAuthorisedException;
import com.Hardik.projects.AirbnbApp.repository.*;
import com.Hardik.projects.AirbnbApp.strategy.PricingService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.Hardik.projects.AirbnbApp.util.AppUtils.getCurrentUser;
import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final InventoryRepository inventoryRepository;
    private final GuestRepository guestRepository;
    private final PaymentCheckOutService paymentCheckOutService;
    private final PricingService pricingService;

    @Value("${frontend.base-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public BookingDto initialiseBooking(BookingRequestDto bookingRequestDto) {
        log.info("Initialize booking for the room");
        Hotel hotel=hotelRepository
                .findById(bookingRequestDto.getHotelId())
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id:"+bookingRequestDto.getHotelId()));

        Room room=roomRepository
                .findById(bookingRequestDto.getRoomId())
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id:"+bookingRequestDto.getRoomId()));

        List<Inventory> inventoryList=inventoryRepository.findAndLockAvailableInventory(bookingRequestDto.getRoomId(),
                bookingRequestDto.getCheckInDate(),bookingRequestDto.getCheckOutDate(),
                bookingRequestDto.getRoomsCount());

        long daysCount= ChronoUnit.DAYS.between(bookingRequestDto.getCheckInDate(),bookingRequestDto.getCheckOutDate())+1;

        if (inventoryList.size()!=daysCount){
            throw new IllegalStateException("Room is not available anymore "+inventoryList.size()+" daysCount "+daysCount);
        }

        //Reserve the room/update the booked count of inventories
        inventoryRepository.initBooking(room.getId(),bookingRequestDto.getCheckInDate(),
                bookingRequestDto.getCheckOutDate(),bookingRequestDto.getRoomsCount());

        //Todo calculate dynamic amount:Done
        BigDecimal priceForOneRoom=pricingService.calculateTotalPrice(inventoryList);
        BigDecimal totalPrice=priceForOneRoom.multiply(BigDecimal.valueOf(bookingRequestDto.getRoomsCount()));

        Booking booking=Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequestDto.getCheckInDate())
                .checkOutDate(bookingRequestDto.getCheckOutDate())
                .user(getCurrentUser())
                .roomCount(bookingRequestDto.getRoomsCount())
                .amount(totalPrice)
                .build();

        booking=bookingRepository.save(booking);
        return modelMapper.map(booking,BookingDto.class);
    }

    public boolean hasBookingExpired(Booking booking){
       return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }


    @Override
    @Transactional
    public BookingDto addGuests(Long bookingId, List<GuestDto> guestDtoList) {
        log.info("Adding the guest with id:{}",bookingId);
        Booking booking=bookingRepository.findById(bookingId)
                .orElseThrow(()->new ResourceNotFoundException("Guest does not exist with booking id:"+bookingId));

        User user=getCurrentUser();

        if (!user.getId().equals(booking.getUser().getId())) {
            throw new UnAuthorisedException(
                    "Booking does not belong to this user with id: " + user.getId());
        }

        if(hasBookingExpired(booking)){
            throw  new IllegalStateException("Booking has already expired");
        }

        if (booking.getBookingStatus()!=BookingStatus.RESERVED){
            throw new IllegalStateException("Booking is not under reserved state so we cannot add guests");
        }

        for (GuestDto guestDto:guestDtoList){
            Guest guest=modelMapper.map(guestDto,Guest.class);
            guest.setUser(getCurrentUser());
            guest=guestRepository.save(guest);
            booking.getGuests().add(guest);
        }

        booking=bookingRepository.save(booking);

        return modelMapper.map(booking,BookingDto.class);
    }

    @Override
    @Transactional
    public String initiatePayments(Long bookingId) {
        Booking booking=bookingRepository.findById(bookingId)
                .orElseThrow(()->new ResourceNotFoundException("Booking does not exist with booking id:"+bookingId));

        User user=getCurrentUser();
        if (!user.getId().equals(booking.getUser().getId())) {
            throw new UnAuthorisedException(
                    "Booking does not belong to this user with id: " + user.getId());
        }
        if(hasBookingExpired(booking)){
            throw  new IllegalStateException("Booking has already expired");
        }

        String sessionUrl=paymentCheckOutService.getCheckOutSession(booking,
                frontendUrl+"/#/payments/success?bookingId="+bookingId,
                frontendUrl+"/#/payments/failure?bookingId="+bookingId);

        booking.setBookingStatus(BookingStatus.PAYMENT_PENDING);
        bookingRepository.save(booking);

        return sessionUrl;
    }


    @Override
    @Transactional
    public void capturePayments(Event event) {
        if ("checkout.session.completed".equals(event.getType())){
            Session session=(Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session!=null){
                String sessionId=session.getId();
                Booking booking=bookingRepository.findByPaymentSessionId(sessionId).orElseThrow(()->new ResourceNotFoundException("Session not found with id"+sessionId));
                booking.setBookingStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);

                inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(),booking.getCheckInDate()
                        ,booking.getCheckOutDate(),booking.getRoomCount());

                inventoryRepository.confirmBooking(booking.getRoom().getId(),booking.getCheckInDate()
                        ,booking.getCheckOutDate(),booking.getRoomCount());

                log.info("Booking confirm with session id:{}",sessionId);
            }else{
                log.warn("Unhandled event type {}",event.getType());
            }
        }
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking=bookingRepository.findById(bookingId)
                .orElseThrow(()->new ResourceNotFoundException("Booking does not exist with booking id:"+bookingId));

        User user=getCurrentUser();
        if (!user.getId().equals(booking.getUser().getId())) {
            throw new UnAuthorisedException(
                    "Booking does not belong to this user with id: " + user.getId());
        }

        if (booking.getBookingStatus()!=BookingStatus.CONFIRMED){
            throw new IllegalStateException("Only confirmed booking can be cancelled");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(),booking.getCheckInDate()
                ,booking.getCheckOutDate(),booking.getRoomCount());

        inventoryRepository.cancelBooking(booking.getRoom().getId(),booking.getCheckInDate()
                ,booking.getCheckOutDate(),booking.getRoomCount());

        //handle the refund

        try {
            Session session=Session.retrieve(booking.getPaymentSessionId());
            RefundCreateParams refundParams=RefundCreateParams.builder()
                    .setPaymentIntent(session.getPaymentIntent())
                    .build();

            Refund.create(refundParams);
        }catch (StripeException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBookingStatus(Long bookingId) {
        Booking booking=bookingRepository.findById(bookingId)
                .orElseThrow(()->new ResourceNotFoundException("Booking does not exist with booking id:"+bookingId));

        log.info("Get booking status with booking id{}",bookingId);

        User user=getCurrentUser();
        if (!user.getId().equals(booking.getUser().getId())) {
            throw new UnAuthorisedException(
                    "Booking does not belong to this user with id: " + user.getId());
        }
        return booking.getBookingStatus().name();
    }

    @Override
    public List<BookingDto> getAllBookingsWithHotelId(Long hotelId) {
        Hotel hotel=hotelRepository
                .findById(hotelId)
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id:"+hotelId));

        User user=getCurrentUser();

        log.info("Getting all bookings for hotel with id"+hotelId);

        if(!user.equals(hotel.getOwner()))
            throw new AccessDeniedException( "You are not the owner of the hotel with id"+hotelId);

        List<Booking> bookings=bookingRepository.findByHotel(hotel);

        return bookings.stream().map((element)->
                modelMapper.map(element,BookingDto.class)).collect(Collectors.toList());
    }

    @Override
    public HotelReportDto getReportsByHotelId(Long hotelId, LocalDateTime startDate, LocalDateTime endDate) {

        Hotel hotel=hotelRepository
                .findById(hotelId)
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id:"+hotelId));

        User user=getCurrentUser();

        log.info("Generating report for hotel with id"+hotelId);

        if(!user.equals(hotel.getOwner()))
            throw new AccessDeniedException( "You are not the owner of the hotel with id"+hotelId);

        List<Booking>bookings=bookingRepository.findByHotelAndCreatedAtBetween(hotel,startDate,endDate);
        Long totalConfirmedBookings=bookings.stream()
                .filter(booking ->booking.getBookingStatus() == BookingStatus.CONFIRMED)
                .count();

        BigDecimal totalRevenueOfConfirmedBookings=bookings.stream()
                .filter(booking -> booking.getBookingStatus()==BookingStatus.CONFIRMED)
                .map(booking -> booking.getAmount())
                .reduce(BigDecimal.ZERO,BigDecimal::add);

        BigDecimal avgRevenue=totalConfirmedBookings==0 ? BigDecimal.ZERO:
                totalRevenueOfConfirmedBookings.divide(BigDecimal.valueOf(totalConfirmedBookings), RoundingMode.HALF_UP);


        return new HotelReportDto(totalConfirmedBookings,totalRevenueOfConfirmedBookings,avgRevenue);
    }

    @Override
    public List<BookingDto> getMyBookings() {
        User user=getCurrentUser();

        return bookingRepository.getBookingsByUser(user).stream()
                .map((element)->modelMapper.map(element,BookingDto.class))
                .collect(Collectors.toList());
    }
}
