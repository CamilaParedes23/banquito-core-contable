package com.banquito.core.accounting.api.dto.api;

public record InstitutionalAccountResponse(
        String functionalCode,
        String name,
        String type,
        String status,
        String accountingCode,
        String accountingName
) {}
