package com.banquito.core.accounting.api.dto.api;

import java.math.BigDecimal;

public record TrialBalanceDetailResponse(
        Integer lineOrder,
        String accountingCode,
        String accountName,
        BigDecimal debitBalance,
        BigDecimal creditBalance
) {}
