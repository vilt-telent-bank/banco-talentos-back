package com.vilt.talentos.dto;

public record AuthResponse(
    String token,
    String refreshToken,
    String name,
    String email,
    String role,
    boolean hasProfile
) {}
