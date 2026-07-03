package com.vilt.talentos.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatusMaquina {
    VAZIO(""),
    EM_PROCESSO_DE_SOLICITACAO("Em processo de solicitação"),
    SOLICITADO("Solicitado"),
    RETIRADO("Retirado"),
    ENVIO_PARA_O_RECURSO("Envio para o recurso"),
    EM_USO("Em Uso"),
    DEVOLVIDO("Devolvido");

    private final String label;
}
