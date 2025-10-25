package com.example.sonata_brain.controller;

import com.example.sonata_brain.dto.UserCreateRequest;
import com.example.sonata_brain.dto.UserResponseDto;
import com.example.sonata_brain.model.User;
import com.example.sonata_brain.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signup(@RequestBody UserCreateRequest createRequest){
        User savedUser = userService.createUser(createRequest);

        UserResponseDto responseDto = new UserResponseDto(savedUser.getId(), savedUser.getEmail(), savedUser.getScope());
        return ResponseEntity.status(201).body(responseDto);
    }


}
