package com.Hardik.projects.AirbnbApp.service;

import com.Hardik.projects.AirbnbApp.dto.RoomDto;

import java.util.List;

public interface RoomService {
    RoomDto createNewRoon(Long hotelId,RoomDto roomDto);
    List<RoomDto> getAllRoomsInHotel(Long hotelId);
    RoomDto getRoomById(Long RoomId);
    void DeleteRoomById(Long RoomId);
    RoomDto updateRoomById(RoomDto roomDto,Long roomId);
}
