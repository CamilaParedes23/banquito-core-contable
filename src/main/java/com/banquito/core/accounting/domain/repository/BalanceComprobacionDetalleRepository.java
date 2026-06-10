package com.banquito.core.accounting.domain.repository;

import com.banquito.core.accounting.domain.model.BalanceComprobacionDetalle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceComprobacionDetalleRepository extends JpaRepository<BalanceComprobacionDetalle, Long> {

    List<BalanceComprobacionDetalle> findByBalanceComprobacionIdOrderByOrdenLineaAsc(Long balanceComprobacionId);
}
