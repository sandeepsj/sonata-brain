package com.example.sonata_brain.dto;

import com.example.sonata_brain.model.Scope;

public class UserResponseDto {
    private Long id;
    private String email;
    private Scope scope;

    public UserResponseDto(){}

    public UserResponseDto(Long id, String email, Scope scope){
        this.id = id;
        this.email = email;
        this.scope = scope;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }
}
