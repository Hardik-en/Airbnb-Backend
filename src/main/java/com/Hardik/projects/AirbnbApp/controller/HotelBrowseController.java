package com.Hardik.projects.AirbnbApp.controller;


import com.Hardik.projects.AirbnbApp.dto.HotelDto;
import com.Hardik.projects.AirbnbApp.dto.HotelInfoDto;
import com.Hardik.projects.AirbnbApp.dto.HotelPriceDto;
import com.Hardik.projects.AirbnbApp.dto.HotelSearchRequestDto;
import com.Hardik.projects.AirbnbApp.service.HotelService;
import com.Hardik.projects.AirbnbApp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
//@Slf4j
@RequestMapping("/hotels")
public class HotelBrowseController {

    private final InventoryService inventoryService;
    private final HotelService hotelService;

    @PostMapping("/search")
    public ResponseEntity<Page<HotelPriceDto>> searchHotels(@RequestBody HotelSearchRequestDto hotelSearchRequestDto){
        Page<HotelPriceDto> page=inventoryService.searchHotels(hotelSearchRequestDto);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoDto> getHotelInfo(@PathVariable Long hotelId){
        return ResponseEntity.ok(hotelService.getHotelInfoById(hotelId));
    }
}
