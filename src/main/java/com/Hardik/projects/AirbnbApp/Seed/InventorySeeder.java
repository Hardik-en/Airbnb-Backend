package com.Hardik.projects.AirbnbApp.Seed;

import com.Hardik.projects.AirbnbApp.entity.Room;
import com.Hardik.projects.AirbnbApp.repository.InventoryRepository;
import com.Hardik.projects.AirbnbApp.repository.RoomRepository;
import com.Hardik.projects.AirbnbApp.service.InventoryService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventorySeeder {

    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    @PostConstruct
    @Transactional
    public void generateInventoryForExistingRooms() {

        if (inventoryRepository.count() > 0) {
            log.info("Inventory already exists. Skipping seed.");
            return;
        }

        List<Room> rooms = roomRepository.findAllWithHotel();

        rooms.forEach(inventoryService::InitializeRoomForAYear);
    }
}