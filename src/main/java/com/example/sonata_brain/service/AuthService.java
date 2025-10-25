package com.example.sonata_brain.service;

import com.example.sonata_brain.dto.LoginRequestDto;
import com.example.sonata_brain.dto.LoginResponseDto;
import com.example.sonata_brain.exception.InvalidPassword;
import com.example.sonata_brain.exception.NotHavingEnoughPermission;
import com.example.sonata_brain.model.Scope;
import com.example.sonata_brain.model.User;
import com.example.sonata_brain.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    public LoginResponseDto login(LoginRequestDto request) {
        // 1. Find the user by email
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Check if the password matches
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidPassword();
            // In a real app, use a specific exception like BadCredentialsException
        }

        if (user.getScope() != Scope.ANY){
            throw new NotHavingEnoughPermission();
        }

        // 3. User is authenticated. Generate tokens.
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // 4. Return the tokens in the response
        return new LoginResponseDto(accessToken, refreshToken);
    }

    public LoginResponseDto refreshToken(String refreshToken) {
        try {
            // 1. Validate the refresh token and extract email
            String userEmail = jwtService.extractUsername(refreshToken);

            // 2. Load the user
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 3. Check if the token is valid (this also checks expiry)
            if (jwtService.isTokenValid(refreshToken, userDetails)) {

                // 4. We need the 'User' object to pass to generateToken
                User user = (User) userDetails;

                // 5. Generate a NEW access token
                String newAccessToken = jwtService.generateToken(user);

                // 6. Return the new access token (and the original refresh token)
                return new LoginResponseDto(newAccessToken, refreshToken);
            } else {
                throw new RuntimeException("Invalid refresh token");
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid refresh token", e);
        }
    }
}