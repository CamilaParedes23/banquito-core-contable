package com.banquito.core.accounting.domain.enums;

import lombok.Getter;

@Getter
public enum EstadoBalanceComprobacionEnum {
    GENERADO("GENERADO"),
    CUADRADO("CUADRADO"),
    DESCADRADO("DESCADRADO"),
    ANULADO("ANULADO");

    private final String value;

    EstadoBalanceComprobacionEnum(String value) {
        this.value = value;
    }
}
