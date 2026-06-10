package com.banquito.core.accounting.domain.enums;

import lombok.Getter;

@Getter
public enum TipoMovimientoContableEnum {
    DEBITO("DEBITO"),
    CREDITO("CREDITO");

    private final String value;

    TipoMovimientoContableEnum(String value) {
        this.value = value;
    }
}
