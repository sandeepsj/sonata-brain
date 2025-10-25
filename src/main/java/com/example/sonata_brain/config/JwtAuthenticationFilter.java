package com.example.sonata_brain.config;

import com.example.sonata_brain.model.Scope;
import com.example.sonata_brain.model.User;
import com.example.sonata_brain.repository.UserRepository;
import com.example.sonata_brain.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Optional;

@Component // Tell Spring to manage this as a bean
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // 1. Check if the header is present and valid
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // If not, pass to the next filter
            return;
        }

        // 2. Extract the token
        final String jwt = authHeader.substring(7); // "Bearer " is 7 chars

        try {
            // 3. Extract the user's email from the token
            final String userEmail = jwtService.extractUsername(jwt);

            // 4. Check if email exists and if user is already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 5. Verify the user still exists in the database and has scope ANY
                Optional<User> userOptional = userRepository.findByEmail(userEmail);
                
                if (userOptional.isEmpty()) {
                    // User no longer exists in database
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found");
                    return;
                }
                
                User user = userOptional.get();
                
                // Verify user has scope ANY
                if (user.getScope() != Scope.ANY) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "User does not have required scope");
                    return;
                }

                // 6. Load the user from the database
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 7. Check if the token is valid
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // 8. THIS IS THE KEY: Create an auth token and set it in the SecurityContext
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // No credentials (password)
                            userDetails.getAuthorities() // Set the user's "scope"
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 9. Update the SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            // Pass to the next filter
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // If token is invalid (expired, bad signature, etc.)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        }
    }
}