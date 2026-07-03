package com.vilt.talentos.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatusRecurso {
    DISPONIVEL("Disponível"),
    AGUARDANDO("Aguardando"),
    ALOCADO("Alocado");

    private final String label;
}
