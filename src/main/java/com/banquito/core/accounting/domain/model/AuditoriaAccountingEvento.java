package com.banquito.core.accounting.domain.model;

import com.banquito.core.accounting.domain.enums.ResultadoAuditoriaAccountingEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "AUDITORIA_ACCOUNTING_EVENTO")
public class AuditoriaAccountingEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "UUID_CORRELACION", length = 36)
    private String uuidCorrelacion;

    @Column(name = "UUID_USUARIO", length = 36)
    private String uuidUsuario;

    @Column(name = "UUID_API_CLIENT", length = 36)
    private String uuidApiClient;

    @Column(name = "SCOPE_USADO", length = 120)
    private String scopeUsado;

    @Column(name = "MODULO", length = 60, nullable = false)
    private String modulo;

    @Column(name = "ACCION", length = 80, nullable = false)
    private String accion;

    @Column(name = "ENTIDAD", length = 80, nullable = false)
    private String entidad;

    @Column(name = "ENTIDAD_ID", length = 80)
    private String entidadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "RESULTADO", length = 15, nullable = false)
    private ResultadoAuditoriaAccountingEnum resultado;

    @Column(name = "DETALLE_JSON", columnDefinition = "json")
    private String detalleJson;

    @Column(name = "FECHA_EVENTO", nullable = false)
    private LocalDateTime fechaEvento;

    public AuditoriaAccountingEvento() {}
    public AuditoriaAccountingEvento(Long id) { this.id = id; }

    public static AuditoriaAccountingEvento registrar(String uuidCorrelacion, String modulo, String accion, String entidad, String entidadId, ResultadoAuditoriaAccountingEnum resultado, String detalleJson) {
        AuditoriaAccountingEvento evento = new AuditoriaAccountingEvento();
        evento.uuidCorrelacion = uuidCorrelacion;
        evento.modulo = modulo;
        evento.accion = accion;
        evento.entidad = entidad;
        evento.entidadId = entidadId;
        evento.resultado = resultado;
        evento.detalleJson = detalleJson;
        evento.fechaEvento = LocalDateTime.now();
        return evento;
    }

    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof AuditoriaAccountingEvento that)) return false; if (id == null || that.id == null) return false; return Objects.equals(id, that.id); }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
