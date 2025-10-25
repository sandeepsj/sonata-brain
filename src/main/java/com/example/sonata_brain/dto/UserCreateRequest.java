package com.example.sonata_brain.dto;

import jakarta.validation.constraints.NotNull;

public class UserCreateRequest {
    @NotNull(message = "Email is required")
    private String email;

    @NotNull(message = "Password is required")
    private String password;

    public String getEmail(){
        return email;
    }

    public  String getPassword(){
        return password;
    }
}
