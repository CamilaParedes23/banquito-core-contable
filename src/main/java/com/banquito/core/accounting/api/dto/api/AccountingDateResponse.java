package com.banquito.core.accounting.api.dto.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AccountingDateResponse(
        Long id,
        LocalDate accountingDate,
        String status,
        LocalTime cutoffTime,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        String observation
) {}
