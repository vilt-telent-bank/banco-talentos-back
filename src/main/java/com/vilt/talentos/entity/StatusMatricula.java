package com.vilt.talentos.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatusMatricula {
    NAO_NECESSARIO("Não Necessário"),
    SOLICITADO_VIA_CHAMADO("Solicitado via chamado"),
    CHAMADO_AGUARDANDO_APROVACAO("Chamado aguardando aprovação"),
    CHAMADO_AGUARDANDO_ATENDIMENTO("Chamado aguardando atendimento"),
    LIBERADA("Liberada");

    private final String label;

    public boolean isAtivo() {
        return this != NAO_NECESSARIO;
    }
}
