package com.banquito.core.accounting.domain.repository;

import com.banquito.core.accounting.domain.model.DetalleAsientoContable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleAsientoContableRepository extends JpaRepository<DetalleAsientoContable, Long> {

    List<DetalleAsientoContable> findByAsientoContableIdOrderByOrdenLineaAsc(Long asientoContableId);
}
