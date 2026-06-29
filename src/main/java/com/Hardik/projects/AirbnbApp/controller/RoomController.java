package com.Hardik.projects.AirbnbApp.controller;

import com.Hardik.projects.AirbnbApp.dto.RoomDto;
import com.Hardik.projects.AirbnbApp.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin/hotels/{hotelId}/rooms")
public class RoomController {
    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomDto> createNewRoom(@RequestBody RoomDto roomDto, @PathVariable Long hotelId){
        RoomDto room=roomService.createNewRoon(hotelId,roomDto);
        return new ResponseEntity<>(room, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RoomDto>> getAllRoomsInHotel(@PathVariable Long hotelId){
        return ResponseEntity.ok(roomService.getAllRoomsInHotel(hotelId));
    }

    @GetMapping("/{RoomId}")
    public ResponseEntity<RoomDto> getRoomById(@PathVariable Long RoomId){
        return ResponseEntity.ok(roomService.getRoomById(RoomId));
    }

    @DeleteMapping("/{RoomId}")
    public ResponseEntity<Void> deleteRoomById(@PathVariable Long RoomId){
        roomService.DeleteRoomById(RoomId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{RoomId}")
    public ResponseEntity<RoomDto> updateRoomById(@RequestBody RoomDto roomDto,@PathVariable Long RoomId){
        return ResponseEntity.ok(roomService.updateRoomById(roomDto,RoomId));
    }

}
