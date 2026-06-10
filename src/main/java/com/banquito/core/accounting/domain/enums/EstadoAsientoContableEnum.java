package com.banquito.core.accounting.domain.enums;

import lombok.Getter;

@Getter
public enum EstadoAsientoContableEnum {
    BORRADOR("BORRADOR"),
    REGISTRADO("REGISTRADO"),
    RECHAZADO("RECHAZADO"),
    REVERSADO("REVERSADO");

    private final String value;

    EstadoAsientoContableEnum(String value) {
        this.value = value;
    }
}
