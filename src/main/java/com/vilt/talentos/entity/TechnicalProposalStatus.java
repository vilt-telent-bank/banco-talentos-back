package com.vilt.talentos.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TechnicalProposalStatus {
    PENDING_SEND("Pendente de envio"),
    SENT_TO_COORDINATOR("Enviado ao coordenador Porto"),
    FOLLOW_UP_REQUIRED("Cobrar retorno"),
    SIGNED("Assinado"),
    SIGNATURE_ERROR("Erro de assinatura");

    private final String description;
}
