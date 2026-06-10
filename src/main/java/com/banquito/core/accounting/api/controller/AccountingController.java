package com.banquito.core.accounting.api.controller;

import com.banquito.core.accounting.api.dto.api.*;
import com.banquito.core.accounting.application.service.AccountingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounting")
@RequiredArgsConstructor
public class AccountingController {
    private final AccountingService service;

    @GetMapping("/chart-of-accounts")
    public List<ChartAccountResponse> listChartOfAccounts() { return service.listarPlanCuentas(); }

    @GetMapping("/chart-of-accounts/{code}")
    public ChartAccountResponse getChartAccount(@PathVariable String code) { return service.obtenerCuentaContable(code); }

    @GetMapping("/institutional-accounts/{functionalCode}")
    public InstitutionalAccountResponse getInstitutionalAccount(@PathVariable String functionalCode) { return service.obtenerCuentaInstitucional(functionalCode); }

    @GetMapping("/accounting-dates/current")
    public AccountingDateResponse getCurrentAccountingDate() { return service.obtenerFechaContableActual(); }

    @PostMapping("/accounting-dates/{date}/open")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountingDateResponse openAccountingDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestBody(required = false) OpenAccountingDateRequest request) { return service.abrirFechaContable(date, request); }

    @PostMapping("/accounting-dates/{date}/close")
    public AccountingDateResponse closeAccountingDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) { return service.cerrarFechaContable(date); }

    @PostMapping("/journal-entries")
    @ResponseStatus(HttpStatus.CREATED)
    public JournalEntryResponse createJournalEntry(@Valid @RequestBody JournalEntryRequest request) { return service.crearAsiento(request); }

    @GetMapping("/journal-entries/{journalEntryUuid}")
    public JournalEntryResponse getJournalEntry(@PathVariable String journalEntryUuid) { return service.obtenerAsiento(journalEntryUuid); }

    @PostMapping("/journal-entries/{journalEntryUuid}/reverse")
    @ResponseStatus(HttpStatus.CREATED)
    public JournalEntryResponse reverseJournalEntry(@PathVariable String journalEntryUuid) { return service.reversarAsiento(journalEntryUuid); }

    @PostMapping("/eod/run")
    @ResponseStatus(HttpStatus.CREATED)
    public EodResponse runEod(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accountingDate) { return service.ejecutarEod(accountingDate); }

    @GetMapping("/eod/{eodUuid}")
    public EodResponse getEod(@PathVariable String eodUuid) { return service.obtenerEod(eodUuid); }

    @GetMapping("/eod/{eodUuid}/status")
    public EodResponse getEodStatus(@PathVariable String eodUuid) { return service.obtenerEod(eodUuid); }

    @GetMapping("/eod/by-date/{accountingDate}")
    public EodResponse getEodByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accountingDate) { return service.obtenerUltimoEodPorFecha(accountingDate); }

    @GetMapping("/trial-balances/{accountingDate}")
    public TrialBalanceResponse getTrialBalance(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accountingDate) { return service.obtenerBalance(accountingDate); }
}
