package com.banquito.core.accounting.api.dto.api;

import java.util.List;

public record JournalEntryListResponse(
        long total,
        int page,
        int size,
        int totalPages,
        List<JournalEntrySummaryResponse> entries
) {}
