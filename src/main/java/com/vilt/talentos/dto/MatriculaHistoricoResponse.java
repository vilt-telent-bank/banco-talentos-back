package com.vilt.talentos.dto;

import java.time.Instant;
import java.util.UUID;

public record MatriculaHistoricoResponse(
    UUID id,
    String valorAnterior,
    String valorNovo,
    String alteradoPorNome,
    Instant alteradoEm
) {}
