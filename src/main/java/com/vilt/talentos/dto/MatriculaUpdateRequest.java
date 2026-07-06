package com.vilt.talentos.dto;

import com.vilt.talentos.entity.StatusMatricula;
import jakarta.validation.constraints.NotNull;

public record MatriculaUpdateRequest(
    @NotNull StatusMatricula statusMatricula
) {}
