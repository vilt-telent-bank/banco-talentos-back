package com.vilt.talentos.dto;

import com.vilt.talentos.entity.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RecursoResponse(
    UUID id,
    String name,
    String email,
    String photoUrl,
    String jobTitle,
    String area,
    DomainStatus status,

    // Seção 1
    StatusRecurso statusRecurso,
    StatusMatricula statusMatricula,
    String numeroMatricula,
    LocalDate dataSolicitacaoMatricula,
    String observacoesMatricula,

    // Seção 2
    boolean possuiMaquinaCliente,
    List<MaquinaResponse> maquinas,

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
    String squadAlocacao,

    // Seção 5
    String contato,
    String endereco,

    Instant createdAt,
    Instant updatedAt
) {}
