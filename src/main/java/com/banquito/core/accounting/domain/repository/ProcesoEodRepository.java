package com.banquito.core.accounting.domain.repository;

import com.banquito.core.accounting.domain.model.ProcesoEod;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcesoEodRepository extends JpaRepository<ProcesoEod, Long> {

    Optional<ProcesoEod> findByUuidEod(String uuidEod);
    Optional<ProcesoEod> findTopByFechaContableOrderByFechaInicioDesc(LocalDate fechaContable);
}
