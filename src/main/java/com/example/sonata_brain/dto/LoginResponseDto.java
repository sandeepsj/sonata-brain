package com.example.sonata_brain.dto;

public record LoginResponseDto(
        String accessToken,
        String refreshToken
) {
}