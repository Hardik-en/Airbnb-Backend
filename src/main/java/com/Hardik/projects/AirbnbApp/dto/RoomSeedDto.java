package com.Hardik.projects.AirbnbApp.dto;

import lombok.Data;

@Data
public class RoomSeedDto {

    private Long hotelId;      // ADD THIS

    private String roomType;

    private String type;

    private Double price;

    private Integer capacity;

    private Integer totalRooms;

    private String[] amenities;

    private String[] photos;
}