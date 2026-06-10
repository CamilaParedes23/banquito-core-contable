package com.banquito.core.accounting.api.dto.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record JournalEntryResponse(
        String journalEntryUuid,
        String correlationId,
        String transactionUuid,
        String originContext,
        String operationType,
        String description,
        LocalDate accountingDate,
        LocalDateTime registeredAt,
        String status,
        String externalReference,
        List<JournalEntryLineResponse> lines
) {}
