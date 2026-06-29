package com.Hardik.projects.AirbnbApp.repository;

import com.Hardik.projects.AirbnbApp.entity.Guest;
import com.Hardik.projects.AirbnbApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestRepository extends JpaRepository<Guest,Long> {
    List<Guest> findByUser(User user);
}
