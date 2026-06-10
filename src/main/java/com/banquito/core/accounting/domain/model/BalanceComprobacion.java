package com.banquito.core.accounting.domain.model;

import com.banquito.core.accounting.domain.enums.EstadoBalanceComprobacionEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "BALANCE_COMPROBACION")
public class BalanceComprobacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "UUID_BALANCE", length = 36, nullable = false)
    private String uuidBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROCESO_EOD_ID", nullable = false)
    private ProcesoEod procesoEod;

    @Column(name = "FECHA_CONTABLE", nullable = false)
    private LocalDate fechaContable;

    @Column(name = "TOTAL_SALDO_DEUDOR", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalSaldoDeudor;

    @Column(name = "TOTAL_SALDO_ACREEDOR", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalSaldoAcreedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", length = 20, nullable = false)
    private EstadoBalanceComprobacionEnum estado;

    @Column(name = "FECHA_GENERACION", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "UUID_DOCUMENTO_CSV", length = 36)
    private String uuidDocumentoCsv;

    @OneToMany(mappedBy = "balanceComprobacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BalanceComprobacionDetalle> detalles = new ArrayList<>();

    public BalanceComprobacion() {}
    public BalanceComprobacion(Long id) { this.id = id; }

    public static BalanceComprobacion crear(ProcesoEod eod, BigDecimal deudor, BigDecimal acreedor) {
        BalanceComprobacion balance = new BalanceComprobacion();
        balance.uuidBalance = UUID.randomUUID().toString();
        balance.procesoEod = eod;
        balance.fechaContable = eod.getFechaContable();
        balance.totalSaldoDeudor = deudor;
        balance.totalSaldoAcreedor = acreedor;
        balance.estado = deudor.compareTo(acreedor) == 0 ? EstadoBalanceComprobacionEnum.CUADRADO : EstadoBalanceComprobacionEnum.DESCADRADO;
        balance.fechaGeneracion = LocalDateTime.now();
        return balance;
    }

    public void agregarDetalle(BalanceComprobacionDetalle detalle) { detalle.setBalanceComprobacion(this); detalles.add(detalle); }

    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof BalanceComprobacion that)) return false; if (id == null || that.id == null) return false; return Objects.equals(id, that.id); }
    @Override public int hashCode() { return Objects.hashCode(id); }
    @Override public String toString() { return "BalanceComprobacion{" + "id=" + id + ", uuidBalance='" + uuidBalance + '\'' + ", fechaContable=" + fechaContable + ", estado=" + estado + '}'; }
}
