package com.banquito.core.accounting.domain.repository;

import com.banquito.core.accounting.domain.enums.EstadoAsientoContableEnum;
import com.banquito.core.accounting.domain.model.AsientoContable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsientoContableRepository extends JpaRepository<AsientoContable, Long> {

    Optional<AsientoContable> findByUuidAsiento(String uuidAsiento);

    Optional<AsientoContable> findFirstByTransaccionUuidAndEstadoOrderByTimestampRegistroAsc(
            String transaccionUuid,
            EstadoAsientoContableEnum estado);

    Optional<AsientoContable> findFirstByUuidCorrelacionAndTipoOperacionAndEstadoOrderByTimestampRegistroAsc(
            String uuidCorrelacion,
            String tipoOperacion,
            EstadoAsientoContableEnum estado);

    List<AsientoContable> findByFechaContableAndEstadoOrderByTimestampRegistroAsc(
            LocalDate fechaContable,
            EstadoAsientoContableEnum estado);

    @Query(value = """
            SELECT a
            FROM AsientoContable a
            WHERE (:dateFrom IS NULL OR a.fechaContable >= :dateFrom)
              AND (:dateTo IS NULL OR a.fechaContable <= :dateTo)
              AND (:operationType IS NULL OR a.tipoOperacion = :operationType)
              AND (:status IS NULL OR a.estado = :status)
              AND (:journalEntryUuid IS NULL OR a.uuidAsiento = :journalEntryUuid)
              AND (:transactionUuid IS NULL OR a.transaccionUuid = :transactionUuid)
              AND (:correlationId IS NULL OR a.uuidCorrelacion = :correlationId)
            ORDER BY a.timestampRegistro DESC, a.id DESC
            """,
            countQuery = """
            SELECT COUNT(a)
            FROM AsientoContable a
            WHERE (:dateFrom IS NULL OR a.fechaContable >= :dateFrom)
              AND (:dateTo IS NULL OR a.fechaContable <= :dateTo)
              AND (:operationType IS NULL OR a.tipoOperacion = :operationType)
              AND (:status IS NULL OR a.estado = :status)
              AND (:journalEntryUuid IS NULL OR a.uuidAsiento = :journalEntryUuid)
              AND (:transactionUuid IS NULL OR a.transaccionUuid = :transactionUuid)
              AND (:correlationId IS NULL OR a.uuidCorrelacion = :correlationId)
            """)
    Page<AsientoContable> searchJournalEntries(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("operationType") String operationType,
            @Param("status") EstadoAsientoContableEnum status,
            @Param("journalEntryUuid") String journalEntryUuid,
            @Param("transactionUuid") String transactionUuid,
            @Param("correlationId") String correlationId,
            Pageable pageable);
}
