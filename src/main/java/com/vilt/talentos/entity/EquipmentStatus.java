package com.vilt.talentos.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EquipmentStatus {
    EMPTY("Vazio"),
    REQUEST_IN_PROGRESS("Em processo de solicitação"),
    REQUESTED("Solicitado"),
    WITHDRAWN("Retirado"),
    SENT_TO_RESOURCE("Envio para o recurso"),
    IN_USE("Em Uso"),
    RETURNED("Devolvido"),
    INACTIVE("Inativo");

    private final String description;
}
