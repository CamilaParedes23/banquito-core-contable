package com.banquito.core.accounting.api.dto.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record JournalEntrySummaryResponse(
        String journalEntryUuid,
        String correlationId,
        String transactionUuid,
        String originContext,
        String operationType,
        String description,
        LocalDate accountingDate,
        LocalDateTime registeredAt,
        String status,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        boolean balanced
) {}
