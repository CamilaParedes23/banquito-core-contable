package com.banquito.core.accounting.domain.repository;

import com.banquito.core.accounting.domain.enums.EstadoJornadaContableEnum;
import com.banquito.core.accounting.domain.model.JornadaContable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface JornadaContableRepository extends JpaRepository<JornadaContable, Long> {

    Optional<JornadaContable> findByFechaContable(LocalDate fechaContable);

    Optional<JornadaContable> findFirstByEstadoOrderByFechaContableDesc(
            EstadoJornadaContableEnum estado);

    Optional<JornadaContable> findFirstByOrderByFechaContableDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select jornada
            from JornadaContable jornada
            where jornada.fechaContable = :fechaContable
            """)
    Optional<JornadaContable> findByFechaContableForUpdate(
            @Param("fechaContable") LocalDate fechaContable);
}
