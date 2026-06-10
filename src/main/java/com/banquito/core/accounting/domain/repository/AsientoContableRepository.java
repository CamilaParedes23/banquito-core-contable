package com.banquito.core.accounting.domain.repository;

import com.banquito.core.accounting.domain.model.AsientoContable;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import com.banquito.core.accounting.domain.enums.EstadoAsientoContableEnum;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsientoContableRepository extends JpaRepository<AsientoContable, Long> {

    Optional<AsientoContable> findByUuidAsiento(String uuidAsiento);
    Optional<AsientoContable> findFirstByUuidCorrelacionAndTipoOperacionAndEstadoOrderByTimestampRegistroAsc(String uuidCorrelacion, String tipoOperacion, EstadoAsientoContableEnum estado);
    List<AsientoContable> findByFechaContableAndEstadoOrderByTimestampRegistroAsc(LocalDate fechaContable, EstadoAsientoContableEnum estado);
}
