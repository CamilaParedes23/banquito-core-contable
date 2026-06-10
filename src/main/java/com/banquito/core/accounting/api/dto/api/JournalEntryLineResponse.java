package com.banquito.core.accounting.api.dto.api;

import java.math.BigDecimal;

public record JournalEntryLineResponse(
        Integer lineOrder,
        String accountingCode,
        String accountingName,
        String movementType,
        BigDecimal amount,
        String reference
) {}
