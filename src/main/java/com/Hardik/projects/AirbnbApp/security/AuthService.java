package com.Hardik.projects.AirbnbApp.security;

import com.Hardik.projects.AirbnbApp.dto.LoginDto;
import com.Hardik.projects.AirbnbApp.dto.SignupRequestDto;
import com.Hardik.projects.AirbnbApp.dto.UserDto;
import com.Hardik.projects.AirbnbApp.entity.User;
import com.Hardik.projects.AirbnbApp.entity.enums.Role;
import com.Hardik.projects.AirbnbApp.exception.ResourceNotFoundException;
import com.Hardik.projects.AirbnbApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UserDto Signup(SignupRequestDto signupRequestDto) {
        return signup(signupRequestDto, Role.GUEST);
    }

    public UserDto signupManager(SignupRequestDto signupRequestDto) {
        return signup(signupRequestDto, Role.HOTEL_MANAGER);
    }

    private UserDto signup(SignupRequestDto signupRequestDto, Role role) {

        if (userRepository.findByEmail(signupRequestDto.getEmail()).isPresent()) {
            throw new RuntimeException(
                    "User is already present with same email id"
            );
        }

        User newUser = modelMapper.map(signupRequestDto, User.class);
        newUser.setRoles(Set.of(role));
        newUser.setPassword(
                passwordEncoder.encode(signupRequestDto.getPassword())
        );

        newUser = userRepository.save(newUser);

        return modelMapper.map(newUser, UserDto.class);
    }

    public String[] login(LoginDto loginDto){
        Authentication authentication=authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(
                loginDto.getEmail(),loginDto.getPassword()
        ));

        User user=(User) authentication.getPrincipal();

        return new String[]{
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user)
        };
    }

    public String refreshToken(String refreshToken){
        Long id= jwtService.getUserIdFromToken(refreshToken);
        User user=userRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("User not found with id:"+id));
        return jwtService.generateAccessToken(user);
    }
}
