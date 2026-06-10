package com.banquito.core.accounting.domain.model;

import com.banquito.core.accounting.domain.enums.EstadoProcesoEodEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "PROCESO_EOD")
public class ProcesoEod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "UUID_EOD", length = 36, nullable = false)
    private String uuidEod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JORNADA_CONTABLE_ID", nullable = false)
    private JornadaContable jornadaContable;

    @Column(name = "FECHA_CONTABLE", nullable = false)
    private LocalDate fechaContable;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", length = 20, nullable = false)
    private EstadoProcesoEodEnum estado;

    @Column(name = "TOTAL_DEBITOS", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalDebitos;

    @Column(name = "TOTAL_CREDITOS", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalCreditos;

    @Column(name = "UUID_DOCUMENTO_REPORTE", length = 36)
    private String uuidDocumentoReporte;

    @Column(name = "MENSAJE_ERROR", length = 1000)
    private String mensajeError;

    @Column(name = "FECHA_INICIO", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "FECHA_FIN")
    private LocalDateTime fechaFin;

    public ProcesoEod() {}
    public ProcesoEod(Long id) { this.id = id; }

    public static ProcesoEod iniciar(JornadaContable jornada) {
        ProcesoEod eod = new ProcesoEod();
        eod.uuidEod = UUID.randomUUID().toString();
        eod.jornadaContable = jornada;
        eod.fechaContable = jornada.getFechaContable();
        eod.estado = EstadoProcesoEodEnum.INICIADO;
        eod.totalDebitos = BigDecimal.ZERO;
        eod.totalCreditos = BigDecimal.ZERO;
        eod.fechaInicio = LocalDateTime.now();
        return eod;
    }

    public void exitoso(BigDecimal debitos, BigDecimal creditos) { this.estado = EstadoProcesoEodEnum.EXITOSO; this.totalDebitos = debitos; this.totalCreditos = creditos; this.fechaFin = LocalDateTime.now(); }
    public void fallido(String mensaje) { this.estado = EstadoProcesoEodEnum.FALLIDO; this.mensajeError = mensaje; this.fechaFin = LocalDateTime.now(); }

    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof ProcesoEod that)) return false; if (id == null || that.id == null) return false; return Objects.equals(id, that.id); }
    @Override public int hashCode() { return Objects.hashCode(id); }
    @Override public String toString() { return "ProcesoEod{" + "id=" + id + ", uuidEod='" + uuidEod + '\'' + ", fechaContable=" + fechaContable + ", estado=" + estado + '}'; }
}
