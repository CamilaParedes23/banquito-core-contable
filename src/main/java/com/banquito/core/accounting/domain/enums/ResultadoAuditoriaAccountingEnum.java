package com.banquito.core.accounting.domain.enums;

import lombok.Getter;

@Getter
public enum ResultadoAuditoriaAccountingEnum {
    OK("OK"),
    ERROR("ERROR"),
    DENEGADO("DENEGADO");

    private final String value;

    ResultadoAuditoriaAccountingEnum(String value) {
        this.value = value;
    }
}
