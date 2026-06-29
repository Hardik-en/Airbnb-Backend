package com.Hardik.projects.AirbnbApp.handlers;

import com.Hardik.projects.AirbnbApp.entity.User;
import com.Hardik.projects.AirbnbApp.entity.enums.Role;
import com.Hardik.projects.AirbnbApp.security.JwtService;
import com.Hardik.projects.AirbnbApp.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserDetailsService userDetailsService;
    private final UserService userService;
    private final JwtService jwtService;
    @Value("${frontend.oauth-success-url}")
    private String oauthSuccessUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OAuth2AuthenticationToken token =
                (OAuth2AuthenticationToken) authentication;

        DefaultOAuth2User oAuth2User =
                (DefaultOAuth2User) token.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        User user;

        try {
            user = (User) userDetailsService.loadUserByUsername(email);

        } catch (UsernameNotFoundException ex) {

            user = User.builder()
                    .name(name)
                    .email(email)
                    .password(UUID.randomUUID().toString())
                    .roles(Set.of(Role.GUEST))
                    .build();

            user = userService.save(user);

            log.info("New OAuth user created: {}", email);
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);

        response.addCookie(cookie);

        getRedirectStrategy().sendRedirect(request, response, oauthSuccessUrl + accessToken);

    }
}
