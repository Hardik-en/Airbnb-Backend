package com.Hardik.projects.AirbnbApp.dto;

import com.Hardik.projects.AirbnbApp.entity.User;
import com.Hardik.projects.AirbnbApp.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
public class GuestDto {
    private Long id;
    private Long userId;
    private String name;
    private LocalDateTime createdAt;
    private Gender gender;
    private Integer age;

}
