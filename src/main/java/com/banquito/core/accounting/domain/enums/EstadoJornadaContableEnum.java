package com.banquito.core.accounting.domain.enums;

import lombok.Getter;

@Getter
public enum EstadoJornadaContableEnum {
    ABIERTA("ABIERTA"),
    EN_CIERRE("EN_CIERRE"),
    CERRADA("CERRADA"),
    BLOQUEADA("BLOQUEADA");

    private final String value;

    EstadoJornadaContableEnum(String value) {
        this.value = value;
    }
}
