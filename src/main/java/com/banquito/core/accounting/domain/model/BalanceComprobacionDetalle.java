package com.banquito.core.accounting.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "BALANCE_COMPROBACION_DETALLE")
public class BalanceComprobacionDetalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BALANCE_COMPROBACION_ID", nullable = false)
    private BalanceComprobacion balanceComprobacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUENTA_CONTABLE_ID", nullable = false)
    private CuentaContable cuentaContable;

    @Column(name = "CODIGO_CONTABLE", length = 30, nullable = false)
    private String codigoContable;

    @Column(name = "NOMBRE_CUENTA", length = 150, nullable = false)
    private String nombreCuenta;

    @Column(name = "SALDO_DEUDOR", precision = 19, scale = 2, nullable = false)
    private BigDecimal saldoDeudor;

    @Column(name = "SALDO_ACREEDOR", precision = 19, scale = 2, nullable = false)
    private BigDecimal saldoAcreedor;

    @Column(name = "ORDEN_LINEA", nullable = false)
    private Integer ordenLinea;

    public BalanceComprobacionDetalle() {}
    public BalanceComprobacionDetalle(Long id) { this.id = id; }

    public static BalanceComprobacionDetalle crear(CuentaContable cuenta, BigDecimal deudor, BigDecimal acreedor, Integer orden) {
        BalanceComprobacionDetalle detalle = new BalanceComprobacionDetalle();
        detalle.cuentaContable = cuenta;
        detalle.codigoContable = cuenta.getCodigoContable();
        detalle.nombreCuenta = cuenta.getNombre();
        detalle.saldoDeudor = deudor;
        detalle.saldoAcreedor = acreedor;
        detalle.ordenLinea = orden;
        return detalle;
    }

    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof BalanceComprobacionDetalle that)) return false; if (id == null || that.id == null) return false; return Objects.equals(id, that.id); }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
