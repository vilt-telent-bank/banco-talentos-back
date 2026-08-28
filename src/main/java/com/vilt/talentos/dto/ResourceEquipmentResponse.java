package com.vilt.talentos.dto;

import com.vilt.talentos.entity.EquipmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ResourceEquipmentResponse(
    UUID id,
    String tag,
    String hostname,
    String assetNumber,
    String brandOs,
    String processor,
    EquipmentStatus status,
    String notes,
    Instant createdAt,
    Instant updatedAt
) {}
