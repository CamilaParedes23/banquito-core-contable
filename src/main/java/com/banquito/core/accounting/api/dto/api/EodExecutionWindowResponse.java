package com.banquito.core.accounting.api.dto.api;

import java.time.LocalDate;
import java.time.LocalTime;

public record EodExecutionWindowResponse(
        String windowCode,
        LocalTime startTime,
        LocalTime cutoffTime,
        LocalTime endTime,
        String actionAfterCutoff,
        LocalDate serverDate,
        LocalTime serverTime,
        boolean normalExecutionAllowed,
        boolean overrideRequired,
        boolean overrideAllowed,
        String restrictionCode,
        String restrictionMessage
) {}
