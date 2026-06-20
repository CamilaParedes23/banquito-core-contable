package com.banquito.core.accounting.api.dto.api;

import java.time.LocalDate;

public record AccountingDailyStatusResponse(
        LocalDate accountingDate,
        boolean accountingDateRegistered,
        boolean activeAccountingDate,
        String accountingDateStatus,
        boolean businessDay,
        String dayType,
        String dayDescription,
        boolean eodGenerated,
        EodResponse eod,
        boolean trialBalanceGenerated,
        TrialBalanceResponse trialBalance,
        EodExecutionWindowResponse eodExecution
) {}
