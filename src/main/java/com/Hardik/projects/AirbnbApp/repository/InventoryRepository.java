package com.Hardik.projects.AirbnbApp.repository;

import com.Hardik.projects.AirbnbApp.entity.Hotel;
import com.Hardik.projects.AirbnbApp.entity.Inventory;
import com.Hardik.projects.AirbnbApp.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory,Long> {

    void deleteByRoom(Room room);

    @Query("""
        SELECT DISTINCT i.hotel
        FROM Inventory i
        WHERE i.city = :city
            AND i.date BETWEEN :startDate AND :endDate
            AND i.closed = false
            AND (i.totalCount - i.bookedCount-i.reservedCount) >= :roomsCount
        GROUP BY i.hotel, i.room
        HAVING COUNT(i.date) = :dateCount
        """)
    Page<Hotel> findHotelsWithAvailableInventory(
        @Param("city") String city,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("roomsCount")Integer roomsCount,
        @Param("dateCount")Long dateCount,
        Pageable pageable
    );

    @Query("""
        SELECT new com.Hardik.projects.AirbnbApp.dto.HotelPriceDto(
            i.hotel,
            AVG(i.price)
        )
        FROM Inventory i
        WHERE LOWER(i.city) = LOWER(:city)
            AND i.date BETWEEN :startDate AND :endDate
            AND i.hotel.active = true
            AND i.closed = false
            AND (i.totalCount - i.bookedCount - i.reservedCount) >= :roomsCount
            AND i.room.id IN (
                SELECT available.room.id
                FROM Inventory available
                WHERE LOWER(available.city) = LOWER(:city)
                    AND available.date BETWEEN :startDate AND :endDate
                    AND available.hotel.active = true
                    AND available.closed = false
                    AND (available.totalCount - available.bookedCount - available.reservedCount) >= :roomsCount
                GROUP BY available.room.id
                HAVING COUNT(DISTINCT available.date) = :dateCount
            )
        GROUP BY i.hotel
        """)
    Page<com.Hardik.projects.AirbnbApp.dto.HotelPriceDto> searchAvailableHotelPrices(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("dateCount") Long dateCount,
            Pageable pageable
    );


    @Query(""" 
          SELECT i
          FROM Inventory i
          where i.room.id = :roomId
                    AND i.date BETWEEN :startDate AND :endDate
                    AND i.closed = false
                    AND (i.totalCount - i.bookedCount-i.reservedCount) >= :roomsCount
          """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockAvailableInventory(
            @Param("roomId") Long roomId,
            @Param("startDate")LocalDate startDate,
            @Param("endDate")LocalDate endDate,
            @Param("roomsCount")Integer roomsCount
    );

    @Query("""
                SELECT i
                FROM Inventory i
                WHERE i.room.id = :roomId
                  AND i.date BETWEEN :startDate AND :endDate
                  AND (i.totalCount - i.bookedCount) >= :numberOfRooms
                  AND i.closed = false
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockReservedInventory(@Param("roomId") Long roomId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("numberOfRooms") int numberOfRooms);

    @Modifying
    @Query("""
       UPDATE Inventory i
       SET i.reservedCount = i.reservedCount + :numberOfRooms,
           i.bookedCount = i.bookedCount + :numberOfRooms
       WHERE i.room.id = :roomId
         AND i.date BETWEEN :startDate AND :endDate
         AND (i.totalCount-i.bookedCount-i.reservedCount)>= :numberOfRooms
         AND i.closed = false
       """)

    void initBooking(@Param("roomId") Long roomId,
                     @Param("startDate")LocalDate startDate,
                     @Param("endDate")LocalDate endDate,
                     @Param("numberOfRooms")Integer roomsCount);

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
       UPDATE Inventory i
       SET i.reservedCount = i.reservedCount - :numberOfRooms,
           i.bookedCount = i.bookedCount + :numberOfRooms
       WHERE i.room.id = :roomId
         AND i.date BETWEEN :startDate AND :endDate
         AND i.reservedCount >= :numberOfRooms
         AND i.closed = false
       """)

    void confirmBooking(@Param("roomId") Long roomId,
                        @Param("startDate")LocalDate startDate,
                        @Param("endDate")LocalDate endDate,
                        @Param("numberOfRooms")Integer roomsCount);


    @Modifying
    @Query("""
       UPDATE Inventory i
       SET i.bookedCount = i.bookedCount - :numberOfRooms
       WHERE i.room.id = :roomId
         AND i.date BETWEEN :startDate AND :endDate
         AND (i.totalCount - i.bookedCount)>= :numberOfRooms
         AND i.reservedCount >= :numberOfRooms
         AND i.closed = false
       """)

    void cancelBooking(@Param("roomId") Long roomId,
                        @Param("startDate")LocalDate startDate,
                        @Param("endDate")LocalDate endDate,
                        @Param("numberOfRooms")Integer roomsCount);



    List<Inventory> findByHotelAndDateBetween(Hotel hotel, LocalDate startDate, LocalDate endDate);

    List<Inventory> findFutureInventoryByRoomId(Long roomId);

    List<Inventory> findByRoomOrderByDate(Room room);

    @Modifying
    @Query("""
       UPDATE Inventory i
       SET i.surgeFactor = :surgeFactor,
           i.closed =:closed
       WHERE i.room.id = :roomId
         AND i.date BETWEEN :startDate AND :endDate
       """)

    void updateInventory(@Param("roomId") Long roomId,
                         @Param("startDate")LocalDate startDate,
                         @Param("endDate")LocalDate endDate,
                         @Param("closed")Boolean closed,
                         @Param("surgeFactor")BigDecimal surgeFactor
                         );

    @Query("""
       SELECT  i
       from Inventory i
       WHERE i.room.id = :roomId
         AND i.date BETWEEN :startDate AND :endDate
       """)

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory>  findInventoryAndLockBeforeUpdate(@Param("roomId") Long roomId,
                         @Param("startDate")LocalDate startDate,
                         @Param("endDate")LocalDate endDate
    );
    @Query("""
      SELECT MIN(i.price)
      FROM Inventory i
      WHERE i.hotel.id = :hotelId
      AND i.date = :date
""")
    BigDecimal findMinPriceByHotelAndDate(
            @Param("hotelId") Long hotelId,
            @Param("date") LocalDate date
    );
}



