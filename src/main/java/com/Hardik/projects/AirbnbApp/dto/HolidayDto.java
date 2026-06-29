package com.Hardik.projects.AirbnbApp.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HolidayDto {

    private LocalDate date;
    private String localName;
    private String name;
}
