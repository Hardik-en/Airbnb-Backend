package com.Hardik.projects.AirbnbApp.dto;

import com.Hardik.projects.AirbnbApp.entity.enums.Gender;
import com.Hardik.projects.AirbnbApp.entity.enums.Role;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private Set<Role> roles;
    private Gender gender;
    private LocalDate dateOfBirth;
}
