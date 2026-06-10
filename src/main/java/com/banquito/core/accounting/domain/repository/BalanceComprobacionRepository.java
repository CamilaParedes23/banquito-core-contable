package com.banquito.core.accounting.domain.repository;

import com.banquito.core.accounting.domain.model.BalanceComprobacion;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceComprobacionRepository extends JpaRepository<BalanceComprobacion, Long> {

    Optional<BalanceComprobacion> findByUuidBalance(String uuidBalance);
    Optional<BalanceComprobacion> findTopByFechaContableOrderByFechaGeneracionDesc(LocalDate fechaContable);
}
