package com.banquito.core.accounting.domain.enums;

import lombok.Getter;

@Getter
public enum EstadoProcesoEodEnum {
    INICIADO("INICIADO"),
    EN_PROCESO("EN_PROCESO"),
    EXITOSO("EXITOSO"),
    FALLIDO("FALLIDO"),
    REVERSADO("REVERSADO");

    private final String value;

    EstadoProcesoEodEnum(String value) {
        this.value = value;
    }
}
