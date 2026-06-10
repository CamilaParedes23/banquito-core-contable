package com.banquito.core.accounting.api.dto.api;

import java.time.LocalTime;

public record OpenAccountingDateRequest(
        LocalTime cutoffTime,
        String observation
) {}
