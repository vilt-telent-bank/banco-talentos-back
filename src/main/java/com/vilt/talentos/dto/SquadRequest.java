package com.vilt.talentos.dto;

import jakarta.validation.constraints.NotBlank;

public record SquadRequest(
    @NotBlank(message = "O nome da squad é obrigatório")
    String name,
    String description,
    String portoCoordinator,
    String projectManager
) {}
