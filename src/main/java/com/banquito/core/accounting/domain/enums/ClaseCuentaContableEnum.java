package com.banquito.core.accounting.domain.enums;

import lombok.Getter;

@Getter
public enum ClaseCuentaContableEnum {
    ACTIVO("ACTIVO"),
    PASIVO("PASIVO"),
    PATRIMONIO("PATRIMONIO"),
    INGRESO("INGRESO"),
    GASTO("GASTO");

    private final String value;

    ClaseCuentaContableEnum(String value) {
        this.value = value;
    }
}
