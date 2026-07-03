package com.vilt.talentos.dto;

import com.vilt.talentos.entity.StatusPropostaTecnica;

import java.time.LocalDate;

public record RecursoUpdateRequest(
    // Seção 1
    String numeroMatricula,
    java.time.LocalDate dataSolicitacaoMatricula,
    String observacoesMatricula,

    // Seção 2
    Boolean possuiMaquinaCliente,

    // Seção 3
    StatusPropostaTecnica statusPropostaTecnica,

    // Seção 4
    String areaContratante,
    String centroCustoContratante,
    LocalDate dataEntradaProjeto,
    Boolean recursoBillable,
    Boolean onboardingPortoRealizado,
    String gerenteProjeto,
    String projetoAlocacao,
    String squadAlocacao
) {}
