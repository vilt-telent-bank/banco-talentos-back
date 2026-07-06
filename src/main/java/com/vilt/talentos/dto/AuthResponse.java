package com.vilt.talentos.dto;

public record AuthResponse(
    String token,
    String name,
    String email,
    String role,
    boolean hasProfile
) {}
