package com.Hardik.projects.AirbnbApp.Seed;

import com.Hardik.projects.AirbnbApp.dto.RoomSeedDto;
import com.Hardik.projects.AirbnbApp.entity.Hotel;
import com.Hardik.projects.AirbnbApp.entity.Room;
import com.Hardik.projects.AirbnbApp.entity.enums.RoomType;
import com.Hardik.projects.AirbnbApp.repository.HotelRepository;
import com.Hardik.projects.AirbnbApp.repository.RoomRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class RoomSeeder implements CommandLineRunner {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {

        if(roomRepository.count() > 0){
            log.info("Rooms already seeded");
            return;
        }

        InputStream inputStream =
                getClass().getResourceAsStream("/seed/rooms.json");

        if(inputStream == null){
            throw new RuntimeException("rooms.json not found");
        }

        List<RoomSeedDto> roomDtos =
                objectMapper.readValue(
                        inputStream,
                        new TypeReference<List<RoomSeedDto>>() {}
                );

        for(RoomSeedDto dto : roomDtos){

            Hotel hotel = hotelRepository
                    .findById(dto.getHotelId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Hotel not found: "
                                            + dto.getHotelId()
                            ));

            Room room = new Room();

            room.setHotel(hotel);

            room.setRoomType(
                    RoomType.valueOf(dto.getRoomType())
            );

            room.setType(dto.getType());

            room.setBasePrice(
                    BigDecimal.valueOf(dto.getPrice())
            );

            room.setCapacity(dto.getCapacity());

            room.setTotalCount(
                    dto.getTotalRooms()
            );

            room.setAmenities(
                    dto.getAmenities()
            );

            room.setPhotos(
                    dto.getPhotos()
            );

            roomRepository.save(room);
        }

        log.info(
                "Room seeding completed. Total Rooms: {}",
                roomRepository.count()
        );
    }
}