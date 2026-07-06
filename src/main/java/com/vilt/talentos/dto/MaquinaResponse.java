package com.vilt.talentos.dto;

import com.vilt.talentos.entity.StatusMaquina;

import java.time.Instant;
import java.util.UUID;

public record MaquinaResponse(
    UUID id,
    String tagNumeroSerie,
    String hostname,
    String numeroAtivo,
    String marcaSistemaOperacional,
    String processador,
    StatusMaquina statusMaquina,
    Instant createdAt,
    Instant updatedAt
) {}
