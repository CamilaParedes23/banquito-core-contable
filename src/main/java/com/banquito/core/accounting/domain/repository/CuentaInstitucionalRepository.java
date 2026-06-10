package com.banquito.core.accounting.domain.repository;

import com.banquito.core.accounting.domain.model.CuentaInstitucional;
import java.util.List;
import java.util.Optional;
import com.banquito.core.accounting.domain.enums.EstadoCuentaInstitucionalEnum;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaInstitucionalRepository extends JpaRepository<CuentaInstitucional, Integer> {

    Optional<CuentaInstitucional> findByCodigoFuncional(String codigoFuncional);
    List<CuentaInstitucional> findByEstadoOrderByCodigoFuncionalAsc(EstadoCuentaInstitucionalEnum estado);
}
