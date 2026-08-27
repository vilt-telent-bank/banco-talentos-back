package com.vilt.talentos.dto;

import jakarta.validation.constraints.NotNull;

public record ResourceEquipmentRequest(
    String tag,
    String hostname,
    String assetNumber,
    String brandOs,
    String processor,
    @NotNull(message = "O status da máquina é obrigatório.")
    String status,
    String notes
) {}
