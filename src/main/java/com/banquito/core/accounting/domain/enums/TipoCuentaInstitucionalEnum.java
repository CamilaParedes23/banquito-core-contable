package com.banquito.core.accounting.domain.enums;

import lombok.Getter;

@Getter
public enum TipoCuentaInstitucionalEnum {
    BANCO_CENTRAL("BANCO_CENTRAL"),
    BOVEDA("BOVEDA"),
    IVA_RETENIDO("IVA_RETENIDO"),
    INGRESOS_COMISION("INGRESOS_COMISION"),
    FONDOS_RESERVADOS("FONDOS_RESERVADOS"),
    CUENTAS_CLIENTES("CUENTAS_CLIENTES");

    private final String value;

    TipoCuentaInstitucionalEnum(String value) {
        this.value = value;
    }
}
