package com.banquito.core.accounting.domain.model;

import com.banquito.core.accounting.domain.enums.TipoMovimientoContableEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "DETALLE_ASIENTO_CONTABLE")
public class DetalleAsientoContable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ASIENTO_CONTABLE_ID", nullable = false)
    private AsientoContable asientoContable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUENTA_CONTABLE_ID", nullable = false)
    private CuentaContable cuentaContable;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_MOVIMIENTO", length = 10, nullable = false)
    private TipoMovimientoContableEnum tipoMovimiento;

    @Column(name = "MONTO", precision = 19, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(name = "REFERENCIA", length = 200)
    private String referencia;

    @Column(name = "ORDEN_LINEA", nullable = false)
    private Integer ordenLinea;

    public DetalleAsientoContable() {}
    public DetalleAsientoContable(Long id) { this.id = id; }

    public static DetalleAsientoContable crear(CuentaContable cuenta, TipoMovimientoContableEnum tipo, BigDecimal monto, String referencia, Integer orden) {
        DetalleAsientoContable detalle = new DetalleAsientoContable();
        detalle.cuentaContable = cuenta;
        detalle.tipoMovimiento = tipo;
        detalle.monto = monto;
        detalle.referencia = referencia;
        detalle.ordenLinea = orden;
        return detalle;
    }

    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof DetalleAsientoContable that)) return false; if (id == null || that.id == null) return false; return Objects.equals(id, that.id); }
    @Override public int hashCode() { return Objects.hashCode(id); }
    @Override public String toString() { return "DetalleAsientoContable{" + "id=" + id + ", tipoMovimiento=" + tipoMovimiento + ", monto=" + monto + ", ordenLinea=" + ordenLinea + '}'; }
}
