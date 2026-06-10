package com.banquito.core.accounting.domain.model;

import com.banquito.core.accounting.domain.enums.EstadoJornadaContableEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "JORNADA_CONTABLE")
public class JornadaContable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "FECHA_CONTABLE", nullable = false)
    private LocalDate fechaContable;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", length = 15, nullable = false)
    private EstadoJornadaContableEnum estado;

    @Column(name = "HORA_CORTE", nullable = false)
    private LocalTime horaCorte;

    @Column(name = "FECHA_APERTURA", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "FECHA_CIERRE")
    private LocalDateTime fechaCierre;

    @Column(name = "OBSERVACION", length = 500)
    private String observacion;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    public JornadaContable() {}
    public JornadaContable(Long id) { this.id = id; }

    public static JornadaContable abrir(LocalDate fecha, LocalTime horaCorte, String observacion) {
        JornadaContable jornada = new JornadaContable();
        jornada.fechaContable = fecha;
        jornada.estado = EstadoJornadaContableEnum.ABIERTA;
        jornada.horaCorte = horaCorte;
        jornada.fechaApertura = LocalDateTime.now();
        jornada.observacion = observacion;
        return jornada;
    }

    public void iniciarCierre() { this.estado = EstadoJornadaContableEnum.EN_CIERRE; }
    public void cerrar(String observacion) { this.estado = EstadoJornadaContableEnum.CERRADA; this.fechaCierre = LocalDateTime.now(); this.observacion = observacion; }

    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof JornadaContable that)) return false; if (id == null || that.id == null) return false; return Objects.equals(id, that.id); }
    @Override public int hashCode() { return Objects.hashCode(id); }
    @Override public String toString() { return "JornadaContable{" + "id=" + id + ", fechaContable=" + fechaContable + ", estado=" + estado + '}'; }
}
