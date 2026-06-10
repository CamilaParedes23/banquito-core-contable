package com.banquito.core.accounting.domain.enums;

import lombok.Getter;

@Getter
public enum NaturalezaSaldoEnum {
    DEUDORA("DEUDORA"),
    ACREEDORA("ACREEDORA");

    private final String value;

    NaturalezaSaldoEnum(String value) {
        this.value = value;
    }
}
