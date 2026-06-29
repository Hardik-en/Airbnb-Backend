package com.Hardik.projects.AirbnbApp.repository;

import com.Hardik.projects.AirbnbApp.dto.BookingDto;
import com.Hardik.projects.AirbnbApp.entity.Booking;
import com.Hardik.projects.AirbnbApp.entity.Hotel;
import com.Hardik.projects.AirbnbApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking,Long> {
    Optional<Booking> findByPaymentSessionId(String sessionId);

    List<Booking> findByHotel(Hotel hotel);

    List<Booking> findByHotelAndCreatedAtBetween(Hotel hotel, LocalDateTime startDateTime,LocalDateTime endDateTime);

    List<Booking> getBookingsByUser(User user);
}
