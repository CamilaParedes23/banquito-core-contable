package com.banquito.core.accounting.api.dto.api;

public record TrialBalanceCsvResponse(
        String filename,
        byte[] content
) {}
