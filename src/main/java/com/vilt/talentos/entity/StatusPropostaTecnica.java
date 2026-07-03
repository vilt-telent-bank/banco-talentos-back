package com.vilt.talentos.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatusPropostaTecnica {
    PENDENTE_DE_ENVIO("Pendente de envio"),
    ENVIADO_AO_COORDENADOR("Enviado ao Coordenador Porto"),
    COBRAR_RETORNO("Cobrar retorno"),
    ASSINADO("Assinado"),
    ERRO_DE_ASSINATURA("Erro de assinatura (verificar)");

    private final String label;
}
