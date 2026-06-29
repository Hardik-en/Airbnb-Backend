package com.Hardik.projects.AirbnbApp.dto;

import com.Hardik.projects.AirbnbApp.entity.HotelContactInfo;
import lombok.Data;
import java.util.List;

@Data
public class HotelSeedDto {

    private String name;

    private String city;

    private String[] photos;

    private String[] amenities;

    private HotelContactInfo contactInfo;

    private boolean active;

    private List<RoomSeedDto> rooms;
}