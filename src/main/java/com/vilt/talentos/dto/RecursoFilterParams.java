package com.vilt.talentos.dto;

import com.vilt.talentos.entity.StatusMatricula;
import com.vilt.talentos.entity.StatusRecurso;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record RecursoFilterParams(
    String nome,
    StatusRecurso statusRecurso,
    StatusMatricula statusMatricula,
    String gerenteProjeto,
    String projeto,
    Boolean billable,
    Boolean onboarding,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataEntradaDe,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataEntradaAte
) {}
