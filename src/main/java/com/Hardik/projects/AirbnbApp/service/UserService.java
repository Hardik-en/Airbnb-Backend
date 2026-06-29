package com.Hardik.projects.AirbnbApp.service;

import com.Hardik.projects.AirbnbApp.dto.BookingDto;
import com.Hardik.projects.AirbnbApp.dto.ProfileUpdateDto;
import com.Hardik.projects.AirbnbApp.dto.UserDto;
import com.Hardik.projects.AirbnbApp.entity.User;

import java.util.List;

public interface UserService {

    User getUserById(Long id);

    User save(User newUser);

    void updateProfile(ProfileUpdateDto profileUpdateDto);

    UserDto getMyProfile();
}
