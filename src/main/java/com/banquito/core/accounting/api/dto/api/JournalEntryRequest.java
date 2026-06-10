package com.banquito.core.accounting.api.dto.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record JournalEntryRequest(
        String correlationId,
        String transactionUuid,
        @NotBlank String originContext,
        @NotBlank String operationType,
        @NotBlank String description,
        @NotNull LocalDate accountingDate,
        String externalReference,
        @NotEmpty List<@Valid JournalEntryLineRequest> lines
) {}
