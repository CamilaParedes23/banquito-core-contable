package com.banquito.core.accounting.domain.enums;

import lombok.Getter;

@Getter
public enum EstadoCuentaContableEnum {
    ACTIVA("ACTIVA"),
    INACTIVA("INACTIVA");

    private final String value;

    EstadoCuentaContableEnum(String value) {
        this.value = value;
    }
}
