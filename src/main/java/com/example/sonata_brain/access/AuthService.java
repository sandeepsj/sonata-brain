package com.example.sonata_brain.access;

import com.example.sonata_brain.access.AccessDtos.LoginRequest;
import com.example.sonata_brain.access.AccessDtos.TokenResponse;
import com.example.sonata_brain.common.error.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuthService {

    // Same message for unknown email and wrong password, so the API doesn't reveal which emails exist (REQ-010.AC6).
    private static final String BAD_CREDENTIALS = "Invalid email or password";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwt) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    public TokenResponse login(LoginRequest request) {
        if (request == null || request.email() == null || request.password() == null) {
            throw ApiException.unauthorized(BAD_CREDENTIALS);
        }
        User user = users.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> ApiException.unauthorized(BAD_CREDENTIALS));
        return tokens(user, jwt.issueRefresh(user.getEmail()));
    }

    /** A new access token with the user's current routes, read from the database (REQ-016.AC2). */
    public TokenResponse refresh(String refreshToken) {
        User user = jwt.verifyRefresh(refreshToken)
                .flatMap(users::findByEmail)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        return tokens(user, refreshToken);
    }

    private TokenResponse tokens(User user, String refreshToken) {
        List<RouteAccess> routes = user.effectiveRoutes();
        return new TokenResponse(jwt.issueAccess(user.getEmail(), routes), refreshToken, routes);
    }
}
