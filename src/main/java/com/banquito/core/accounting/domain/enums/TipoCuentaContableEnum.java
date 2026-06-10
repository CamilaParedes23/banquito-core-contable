package com.banquito.core.accounting.domain.enums;

import lombok.Getter;

@Getter
public enum TipoCuentaContableEnum {
    ESTRUCTURAL("ESTRUCTURAL"),
    DETALLE("DETALLE");

    private final String value;

    TipoCuentaContableEnum(String value) {
        this.value = value;
    }
}
