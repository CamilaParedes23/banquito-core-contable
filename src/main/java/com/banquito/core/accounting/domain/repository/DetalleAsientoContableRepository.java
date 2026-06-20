package com.banquito.core.accounting.domain.repository;

import com.banquito.core.accounting.domain.model.DetalleAsientoContable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DetalleAsientoContableRepository extends JpaRepository<DetalleAsientoContable, Long> {

    List<DetalleAsientoContable> findByAsientoContableIdOrderByOrdenLineaAsc(Long asientoContableId);

    List<DetalleAsientoContable> findByAsientoContableIdInOrderByAsientoContableIdAscOrdenLineaAsc(
            Collection<Long> asientoContableIds);
}
