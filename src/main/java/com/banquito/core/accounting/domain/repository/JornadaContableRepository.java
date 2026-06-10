package com.banquito.core.accounting.domain.repository;

import com.banquito.core.accounting.domain.model.JornadaContable;
import java.util.Optional;
import java.time.LocalDate;
import com.banquito.core.accounting.domain.enums.EstadoJornadaContableEnum;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JornadaContableRepository extends JpaRepository<JornadaContable, Long> {

    Optional<JornadaContable> findByFechaContable(LocalDate fechaContable);
    Optional<JornadaContable> findFirstByEstadoOrderByFechaContableDesc(EstadoJornadaContableEnum estado);
}
