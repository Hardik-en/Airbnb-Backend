package com.Hardik.projects.AirbnbApp.service;

import com.Hardik.projects.AirbnbApp.dto.HotelPriceDto;
import com.Hardik.projects.AirbnbApp.dto.HotelSearchRequestDto;
import com.Hardik.projects.AirbnbApp.dto.InventoryDto;
import com.Hardik.projects.AirbnbApp.dto.UpdateInventoryDto;
import com.Hardik.projects.AirbnbApp.entity.Room;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InventoryService {

    void InitializeRoomForAYear(Room room);
    void deleteAllInventories(Room room);
    Page<HotelPriceDto> searchHotels(HotelSearchRequestDto hotelSearchRequestDto);
    List<InventoryDto> getAllInventoriesByRoom(Long roomId);
    void updateInventory(Long roomId, UpdateInventoryDto updateInventoryDto);
}
