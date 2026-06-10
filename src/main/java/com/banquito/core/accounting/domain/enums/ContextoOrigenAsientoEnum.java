package com.banquito.core.accounting.domain.enums;

import lombok.Getter;

@Getter
public enum ContextoOrigenAsientoEnum {
    ACCOUNT("ACCOUNT"),
    SWITCH("SWITCH"),
    VENTANILLA("VENTANILLA"),
    BANCA_WEB("BANCA_WEB"),
    EOD("EOD"),
    CORE_INTERNO("CORE_INTERNO");

    private final String value;

    ContextoOrigenAsientoEnum(String value) {
        this.value = value;
    }
}
