package com.Hardik.projects.AirbnbApp.dto;

import com.Hardik.projects.AirbnbApp.entity.enums.Gender;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateDto {
    private String name;
    private LocalDate dateOfBirth;
    private Gender gender;
}
