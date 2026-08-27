package com.vilt.talentos.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RegistrationStatus {
    NOT_REQUIRED("Não Necessário"),
    REQUESTED_VIA_TICKET("Solicitado via chamado"),
    TICKET_AWAITING_APPROVAL("Chamado aguardando aprovação"),
    TICKET_AWAITING_SERVICE("Chamado aguardando atendimento"),
    RELEASED("Liberada");

    private final String description;
}
