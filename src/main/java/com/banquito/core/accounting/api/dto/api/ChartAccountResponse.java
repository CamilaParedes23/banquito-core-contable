package com.banquito.core.accounting.api.dto.api;

import java.math.BigDecimal;

public record ChartAccountResponse(
        String code,
        String name,
        String accountClass,
        String balanceNature,
        String accountType,
        String parentCode,
        Integer level,
        Boolean allowsMovements,
        BigDecimal currentBalance,
        String status
) {}
