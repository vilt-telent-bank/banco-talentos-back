package com.vilt.talentos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record ProjectRequest(
    @NotBlank(message = "O nome do projeto é obrigatório")
    String name,
    String description,
    @Valid SquadRequest squad,
    List<UUID> squadIds
) {}
