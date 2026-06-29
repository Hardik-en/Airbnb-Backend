package com.Hardik.projects.AirbnbApp.dto;

import lombok.Data;

import com.Hardik.projects.AirbnbApp.entity.enums.RoomType;

import java.math.BigDecimal;

@Data
public class RoomDto {

    private Long id;
    private String type;
    private RoomType roomType;
    private BigDecimal basePrice;
    private String[] amenities;
    private String[] photos;
    private Integer totalCount;
    private Integer capacity;
}
