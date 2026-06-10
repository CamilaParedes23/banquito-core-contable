package com.banquito.core.accounting.domain.model;

import com.banquito.core.accounting.domain.enums.EstadoCuentaInstitucionalEnum;
import com.banquito.core.accounting.domain.enums.TipoCuentaInstitucionalEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "CUENTA_INSTITUCIONAL")
public class CuentaInstitucional {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUENTA_CONTABLE_ID", nullable = false)
    private CuentaContable cuentaContable;

    @Column(name = "CODIGO_FUNCIONAL", length = 50, nullable = false)
    private String codigoFuncional;

    @Column(name = "NOMBRE", length = 150, nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_CUENTA", length = 40, nullable = false)
    private TipoCuentaInstitucionalEnum tipoCuenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", length = 15, nullable = false)
    private EstadoCuentaInstitucionalEnum estado;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "FECHA_ACTUALIZACION", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    public CuentaInstitucional() {}
    public CuentaInstitucional(Integer id) { this.id = id; }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof CuentaInstitucional that)) return false; if (id == null || that.id == null) return false; return Objects.equals(id, that.id); }
    @Override public int hashCode() { return Objects.hashCode(id); }
    @Override public String toString() { return "CuentaInstitucional{" + "id=" + id + ", codigoFuncional='" + codigoFuncional + '\'' + ", estado=" + estado + '}'; }
}
