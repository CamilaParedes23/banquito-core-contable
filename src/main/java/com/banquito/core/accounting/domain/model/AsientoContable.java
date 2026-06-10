package com.banquito.core.accounting.domain.model;

import com.banquito.core.accounting.domain.enums.ContextoOrigenAsientoEnum;
import com.banquito.core.accounting.domain.enums.EstadoAsientoContableEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ASIENTO_CONTABLE")
public class AsientoContable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "UUID_ASIENTO", length = 36, nullable = false)
    private String uuidAsiento;

    @Column(name = "UUID_CORRELACION", length = 36, nullable = false)
    private String uuidCorrelacion;

    @Column(name = "TRANSACCION_UUID", length = 36)
    private String transaccionUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "CONTEXTO_ORIGEN", length = 40, nullable = false)
    private ContextoOrigenAsientoEnum contextoOrigen;

    @Column(name = "TIPO_OPERACION", length = 50, nullable = false)
    private String tipoOperacion;

    @Column(name = "DESCRIPCION", length = 500, nullable = false)
    private String descripcion;

    @Column(name = "FECHA_CONTABLE", nullable = false)
    private LocalDate fechaContable;

    @Column(name = "TIMESTAMP_REGISTRO", nullable = false)
    private LocalDateTime timestampRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", length = 20, nullable = false)
    private EstadoAsientoContableEnum estado;

    @Column(name = "REFERENCIA_EXTERNA", length = 120)
    private String referenciaExterna;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    @OneToMany(mappedBy = "asientoContable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleAsientoContable> detalles = new ArrayList<>();

    public AsientoContable() {}
    public AsientoContable(Long id) { this.id = id; }

    public static AsientoContable crear(String uuidCorrelacion, String transaccionUuid, ContextoOrigenAsientoEnum contexto, String tipoOperacion, String descripcion, LocalDate fechaContable, String referenciaExterna) {
        AsientoContable asiento = new AsientoContable();
        asiento.uuidAsiento = UUID.randomUUID().toString();
        asiento.uuidCorrelacion = uuidCorrelacion == null || uuidCorrelacion.isBlank() ? UUID.randomUUID().toString() : uuidCorrelacion;
        asiento.transaccionUuid = transaccionUuid;
        asiento.contextoOrigen = contexto;
        asiento.tipoOperacion = tipoOperacion;
        asiento.descripcion = descripcion;
        asiento.fechaContable = fechaContable;
        asiento.timestampRegistro = LocalDateTime.now();
        asiento.fechaCreacion = LocalDateTime.now();
        asiento.estado = EstadoAsientoContableEnum.REGISTRADO;
        asiento.referenciaExterna = referenciaExterna;
        return asiento;
    }

    public void agregarDetalle(DetalleAsientoContable detalle) {
        detalle.setAsientoContable(this);
        detalles.add(detalle);
    }

    public void marcarReversado() { this.estado = EstadoAsientoContableEnum.REVERSADO; }

    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof AsientoContable that)) return false; if (id == null || that.id == null) return false; return Objects.equals(id, that.id); }
    @Override public int hashCode() { return Objects.hashCode(id); }
    @Override public String toString() { return "AsientoContable{" + "id=" + id + ", uuidAsiento='" + uuidAsiento + '\'' + ", fechaContable=" + fechaContable + ", estado=" + estado + '}'; }
}
