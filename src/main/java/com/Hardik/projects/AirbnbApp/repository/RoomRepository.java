package com.Hardik.projects.AirbnbApp.repository;

import com.Hardik.projects.AirbnbApp.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room,Long> {

    @Query("""
        SELECT r
        FROM Room r
        JOIN FETCH r.hotel
    """)
    List<Room> findAllWithHotel();
}
