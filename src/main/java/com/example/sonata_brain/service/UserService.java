package com.example.sonata_brain.service;

import com.example.sonata_brain.dto.UserCreateRequest;
import com.example.sonata_brain.model.User;
import com.example.sonata_brain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Transactional
    public User createUser(UserCreateRequest request ){
        try {
            logger.info("Creating new user entry for email: {}", request.getEmail());

            String hashedPassword = passwordEncoder.encode(request.getPassword());

            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(hashedPassword);
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e){
            throw new RuntimeException("Email already exists " + request.getEmail());
        }
    }
}
