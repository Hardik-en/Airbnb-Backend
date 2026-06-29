package com.Hardik.projects.AirbnbApp.controller;
import com.Hardik.projects.AirbnbApp.dto.LoginDto;
import com.Hardik.projects.AirbnbApp.dto.LoginResponseDto;
import com.Hardik.projects.AirbnbApp.dto.SignupRequestDto;
import com.Hardik.projects.AirbnbApp.dto.UserDto;
import com.Hardik.projects.AirbnbApp.entity.User;
import com.Hardik.projects.AirbnbApp.security.AuthService;
import com.Hardik.projects.AirbnbApp.security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signup(@RequestBody SignupRequestDto signupRequestDto){
        return new ResponseEntity<>(authService.Signup(signupRequestDto), HttpStatus.CREATED);
    }

    @PostMapping("/signup/manager")
    public ResponseEntity<UserDto> signupManager(@RequestBody SignupRequestDto signupRequestDto){
        return new ResponseEntity<>(authService.signupManager(signupRequestDto), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginDto loginDto, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        String[] tokens=authService.login(loginDto);
        Cookie cookie=new Cookie("refreshToken",tokens[1]);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(30 * 24 * 60 * 60);
        httpServletResponse.addCookie(cookie);
        return ResponseEntity.ok(new LoginResponseDto(tokens[0]));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refreshToken(HttpServletRequest httpServletRequest){
        String refreshToken = Arrays.stream(Optional.ofNullable(httpServletRequest.getCookies()).orElse(new Cookie[0]))
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() ->
                        new AuthenticationServiceException("Refresh Token not found inside cookie"));

        String accessToken= authService.refreshToken(refreshToken);
        return ResponseEntity.ok(new LoginResponseDto(accessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response){
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.noContent().build();
    }

}
