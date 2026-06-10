package com.banquito.core.accounting.domain.repository;

import com.banquito.core.accounting.domain.model.CuentaContable;
import java.util.List;
import java.util.Optional;
import com.banquito.core.accounting.domain.enums.EstadoCuentaContableEnum;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaContableRepository extends JpaRepository<CuentaContable, Integer> {

    Optional<CuentaContable> findByCodigoContable(String codigoContable);
    List<CuentaContable> findByEstadoOrderByCodigoContableAsc(EstadoCuentaContableEnum estado);
}
