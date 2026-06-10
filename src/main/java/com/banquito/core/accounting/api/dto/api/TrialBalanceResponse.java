package com.banquito.core.accounting.api.dto.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TrialBalanceResponse(
        String balanceUuid,
        LocalDate accountingDate,
        BigDecimal totalDebitBalance,
        BigDecimal totalCreditBalance,
        String status,
        LocalDateTime generatedAt,
        String csvDocumentUuid,
        List<TrialBalanceDetailResponse> details
) {}
