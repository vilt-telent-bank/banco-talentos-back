package com.vilt.talentos.dto;

import com.vilt.talentos.entity.EquipmentStatus;
import jakarta.validation.constraints.NotNull;

public record ResourceEquipmentRequest(
    String tag,
    String hostname,
    String assetNumber,
    String brandOs,
    String processor,
    @NotNull(message = "O status da máquina é obrigatório.")
    EquipmentStatus status,
    String notes
) {}
