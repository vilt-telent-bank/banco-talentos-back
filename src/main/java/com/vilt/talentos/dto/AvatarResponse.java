package com.vilt.talentos.dto;

import com.vilt.talentos.entity.AvatarType;

public record AvatarResponse(
        AvatarType type,
        String value
) {}
