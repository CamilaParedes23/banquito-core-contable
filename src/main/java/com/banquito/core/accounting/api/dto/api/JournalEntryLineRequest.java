package com.banquito.core.accounting.api.dto.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record JournalEntryLineRequest(
        String accountingCode,
        String institutionalAccountCode,
        @NotBlank String movementType,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String reference,
        Integer lineOrder
) {}
