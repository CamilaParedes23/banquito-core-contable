package com.banquito.core.accounting.domain.model;

import com.banquito.core.accounting.domain.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "CUENTA_CONTABLE")
public class CuentaContable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Integer id;

    @Column(name = "CODIGO_CONTABLE", length = 30, nullable = false)
    private String codigoContable;

    @Column(name = "NOMBRE", length = 150, nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "CLASE", length = 20, nullable = false)
    private ClaseCuentaContableEnum clase;

    @Enumerated(EnumType.STRING)
    @Column(name = "NATURALEZA_SALDO", length = 15, nullable = false)
    private NaturalezaSaldoEnum naturalezaSaldo;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_CUENTA", length = 15, nullable = false)
    private TipoCuentaContableEnum tipoCuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUENTA_PADRE_ID")
    private CuentaContable cuentaPadre;

    @Column(name = "NIVEL", nullable = false)
    private Integer nivel;

    @Column(name = "PERMITE_MOVIMIENTOS", nullable = false)
    private Boolean permiteMovimientos;

    @Column(name = "SALDO_ACTUAL", precision = 19, scale = 2, nullable = false)
    private BigDecimal saldoActual;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", length = 15, nullable = false)
    private EstadoCuentaContableEnum estado;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "FECHA_ACTUALIZACION", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    public CuentaContable() {}

    public CuentaContable(Integer id) { this.id = id; }

    public void aplicarMovimiento(TipoMovimientoContableEnum tipoMovimiento, BigDecimal monto) {
        if (saldoActual == null) saldoActual = BigDecimal.ZERO;
        boolean aumenta = (naturalezaSaldo == NaturalezaSaldoEnum.DEUDORA && tipoMovimiento == TipoMovimientoContableEnum.DEBITO)
                || (naturalezaSaldo == NaturalezaSaldoEnum.ACREEDORA && tipoMovimiento == TipoMovimientoContableEnum.CREDITO);
        saldoActual = aumenta ? saldoActual.add(monto) : saldoActual.subtract(monto);
    }

    public boolean esDetalleActivaMovible() {
        return tipoCuenta == TipoCuentaContableEnum.DETALLE
                && Boolean.TRUE.equals(permiteMovimientos)
                && estado == EstadoCuentaContableEnum.ACTIVA;
    }

    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof CuentaContable that)) return false; if (id == null || that.id == null) return false; return Objects.equals(id, that.id); }
    @Override public int hashCode() { return Objects.hashCode(id); }
    @Override public String toString() { return "CuentaContable{" + "id=" + id + ", codigoContable='" + codigoContable + '\'' + ", nombre='" + nombre + '\'' + ", estado=" + estado + '}'; }
}
