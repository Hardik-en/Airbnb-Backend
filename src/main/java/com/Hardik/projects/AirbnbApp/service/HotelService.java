package com.Hardik.projects.AirbnbApp.service;

import com.Hardik.projects.AirbnbApp.dto.HotelDto;
import com.Hardik.projects.AirbnbApp.dto.HotelInfoDto;

import java.util.List;

public interface HotelService {

    HotelDto createNewHotel(HotelDto hotelDto);
    HotelDto getHotelById(Long id);
    HotelDto updateHotelById(Long id,HotelDto hotelDto);
    void deleteHotelById(Long id);
    void activateHotel(Long id);
    HotelInfoDto getHotelInfoById(Long hotelId);
    List<HotelDto> getAllHotels();
}
