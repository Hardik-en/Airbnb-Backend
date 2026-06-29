package com.Hardik.projects.AirbnbApp.service;

import com.Hardik.projects.AirbnbApp.dto.*;
import com.stripe.model.Event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public interface BookingService {
    BookingDto initialiseBooking(BookingRequestDto bookingRequestDto);

    BookingDto addGuests(Long bookingId, List<GuestDto> guestDtoList);

    String initiatePayments(Long bookingId);

    void capturePayments(Event event);

    void cancelBooking(Long bookingId);

    String getBookingStatus(Long bookingId);

    List<BookingDto> getAllBookingsWithHotelId(Long hotelId);

    HotelReportDto getReportsByHotelId(Long hotelId, LocalDateTime startDate, LocalDateTime endDate);

    List<BookingDto> getMyBookings();


}
