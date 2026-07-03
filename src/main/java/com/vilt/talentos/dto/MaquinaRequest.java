package com.vilt.talentos.dto;

import com.vilt.talentos.entity.StatusMaquina;
import jakarta.validation.constraints.NotNull;

public record MaquinaRequest(
    String tagNumeroSerie,
    String hostname,
    String numeroAtivo,
    String marcaSistemaOperacional,
    String processador,
    @NotNull StatusMaquina statusMaquina
) {}
