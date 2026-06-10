package com.banquito.core.accounting.domain.enums;

import lombok.Getter;

@Getter
public enum EstadoCuentaInstitucionalEnum {
    ACTIVA("ACTIVA"),
    INACTIVA("INACTIVA");

    private final String value;

    EstadoCuentaInstitucionalEnum(String value) {
        this.value = value;
    }
}
