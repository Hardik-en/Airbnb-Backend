package com.Hardik.projects.AirbnbApp.service;

import com.Hardik.projects.AirbnbApp.dto.BookingDto;
import com.Hardik.projects.AirbnbApp.dto.ProfileUpdateDto;
import com.Hardik.projects.AirbnbApp.dto.UserDto;
import com.Hardik.projects.AirbnbApp.entity.User;
import com.Hardik.projects.AirbnbApp.exception.ResourceNotFoundException;
import com.Hardik.projects.AirbnbApp.repository.BookingRepository;
import com.Hardik.projects.AirbnbApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.Hardik.projects.AirbnbApp.util.AppUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("User not found with id:"+id));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    @Override
    public User save(User newUser) {
        return userRepository.save(newUser);
    }

    @Override
    public void updateProfile(ProfileUpdateDto profileUpdateDto) {
        User user=getCurrentUser();
        if(profileUpdateDto.getDateOfBirth()!=null)user.setDateOfBirth(profileUpdateDto.getDateOfBirth());
        if (profileUpdateDto.getGender()!=null)user.setGender(profileUpdateDto.getGender());
        if (profileUpdateDto.getName()!=null)user.setName(profileUpdateDto.getName());

        userRepository.save(user);
    }


    @Override
    public UserDto getMyProfile() {
        User user=getCurrentUser();
        log.info("Getting the profile for user with id {}:",user.getId());
        return modelMapper.map(user,UserDto.class);
    }
}

