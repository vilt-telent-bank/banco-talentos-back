package com.vilt.talentos.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourceStatus {
    AVAILABLE("Disponível"),
    WAITING("Aguardando"),
    ALLOCATED("Alocado");

    private final String description;

    public static ResourceStatus fromRegistrationStatus(RegistrationStatus registrationStatus) {
        if (registrationStatus == null || registrationStatus == RegistrationStatus.NOT_REQUIRED) {
            return AVAILABLE;
        }
        if (registrationStatus == RegistrationStatus.RELEASED) {
            return ALLOCATED;
        }
        return WAITING;
    }
}
