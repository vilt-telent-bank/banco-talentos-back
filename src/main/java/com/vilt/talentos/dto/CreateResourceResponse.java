package com.vilt.talentos.dto;

import java.util.UUID;

public record CreateResourceResponse(
        UUID profileId,
        UUID userId,
        String name,
        String email
) {
}
