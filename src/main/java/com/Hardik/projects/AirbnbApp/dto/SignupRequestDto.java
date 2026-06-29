package com.Hardik.projects.AirbnbApp.dto;

import com.Hardik.projects.AirbnbApp.entity.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class SignupRequestDto {
    private String name;
    private String email;
    private String password;

}
