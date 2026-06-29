package com.Hardik.projects.AirbnbApp.Seed;

import com.Hardik.projects.AirbnbApp.dto.HotelSeedDto;
import com.Hardik.projects.AirbnbApp.entity.Hotel;
import com.Hardik.projects.AirbnbApp.entity.Room;
import com.Hardik.projects.AirbnbApp.entity.User;
import com.Hardik.projects.AirbnbApp.entity.enums.RoomType;
import com.Hardik.projects.AirbnbApp.repository.HotelRepository;
import com.Hardik.projects.AirbnbApp.repository.RoomRepository;
import com.Hardik.projects.AirbnbApp.repository.UserRepository;
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
@Order(1)
public class HotelDataSeeder implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {

        if (hotelRepository.count() > 0) {
            log.info("Hotels already seeded. Skipping...");
            return;
        }

        User owner = userRepository.findById(1L)
                .orElseThrow(() ->
                        new RuntimeException("Owner with id 1 not found"));

        InputStream inputStream =
                getClass().getResourceAsStream("/seed/hotels.json");

        if (inputStream == null) {
            throw new RuntimeException(
                    "hotels.json not found in resources/seed folder"
            );
        }

        List<HotelSeedDto> hotelDtos =
                objectMapper.readValue(
                        inputStream,
                        new TypeReference<List<HotelSeedDto>>() {
                        });

        for (HotelSeedDto dto : hotelDtos) {

            Hotel hotel = new Hotel();

            hotel.setName(dto.getName());
            hotel.setCity(dto.getCity());
            hotel.setPhotos(dto.getPhotos());
            hotel.setAmenities(dto.getAmenities());
            hotel.setContactInfo(dto.getContactInfo());
            hotel.setActive(dto.isActive());
            hotel.setOwner(owner);

            Hotel savedHotel = hotelRepository.save(hotel);

            if (dto.getRooms() == null || dto.getRooms().isEmpty()) {
                continue;
            }

            List<Room> rooms = dto.getRooms()
                    .stream()
                    .map(roomDto -> {

                        Room room = new Room();

                        room.setHotel(savedHotel);

                        room.setRoomType(
                                RoomType.valueOf(
                                        roomDto.getRoomType()
                                )
                        );

                        room.setType(
                                roomDto.getType()
                        );

                        room.setBasePrice(
                                BigDecimal.valueOf(
                                        roomDto.getPrice()
                                )
                        );

                        room.setCapacity(
                                roomDto.getCapacity()
                        );

                        room.setTotalCount(
                                roomDto.getTotalRooms()
                        );

                        room.setAmenities(
                                roomDto.getAmenities()
                        );

                        room.setPhotos(
                                roomDto.getPhotos()
                        );

                        return room;
                    })
                    .toList();

            roomRepository.saveAll(rooms);

            log.info(
                    "Seeded hotel: {} with {} room categories",
                    savedHotel.getName(),
                    rooms.size()
            );
        }

        log.info(
                "Hotel seeding completed successfully. Total Hotels: {}",
                hotelRepository.count()
        );
    }
}