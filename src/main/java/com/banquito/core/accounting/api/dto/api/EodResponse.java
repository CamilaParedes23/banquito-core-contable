package com.banquito.core.accounting.api.dto.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EodResponse(
        String eodUuid,
        LocalDate accountingDate,
        String status,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        String reportDocumentUuid,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDate nextAccountingDate,
        String nextAccountingDateStatus
) {}
