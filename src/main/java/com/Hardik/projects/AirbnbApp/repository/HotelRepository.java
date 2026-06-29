package com.Hardik.projects.AirbnbApp.repository;

import com.Hardik.projects.AirbnbApp.entity.Hotel;
import com.Hardik.projects.AirbnbApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel,Long> {
    List<Hotel> findByOwner(User user);
}
