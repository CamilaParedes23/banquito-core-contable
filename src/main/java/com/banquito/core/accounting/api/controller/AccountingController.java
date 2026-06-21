package com.banquito.core.accounting.api.controller;

import com.banquito.core.accounting.api.dto.api.*;
import com.banquito.core.accounting.application.service.AccountingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounting")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_SEGURIDAD', 'OPERADOR_CONTABLE') or hasAuthority('SCOPE_core.accounting.entry.create') or hasAuthority('SCOPE_core.accounting.eod.run')")
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

    @GetMapping("/daily-status/{accountingDate}")
    public AccountingDailyStatusResponse getDailyStatus(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accountingDate) {
        return service.obtenerEstadoDiario(accountingDate);
    }

    @PostMapping("/accounting-dates/{date}/open")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountingDateResponse openAccountingDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestBody(required = false) OpenAccountingDateRequest request) { return service.abrirFechaContable(date, request); }

    @PostMapping("/accounting-dates/{date}/close")
    public AccountingDateResponse closeAccountingDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) { return service.cerrarFechaContable(date); }

    @PostMapping("/accounting-dates/recover-next")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountingDateResponse recoverNextAccountingDate() {
        return service.recuperarSiguienteFechaContable();
    }

    @PostMapping("/journal-entries")
    @ResponseStatus(HttpStatus.CREATED)
    public JournalEntryResponse createJournalEntry(@Valid @RequestBody JournalEntryRequest request) { return service.crearAsiento(request); }

    @GetMapping("/journal-entries")
    @PreAuthorize("hasAnyRole('ADMIN_SEGURIDAD', 'OPERADOR_CONTABLE')")
    public JournalEntryListResponse listJournalEntries(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String journalEntryUuid,
            @RequestParam(required = false) String transactionUuid,
            @RequestParam(required = false) String correlationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listarAsientos(
                dateFrom,
                dateTo,
                operationType,
                status,
                journalEntryUuid,
                transactionUuid,
                correlationId,
                page,
                size);
    }

    @GetMapping("/journal-entries/by-transaction/{transactionUuid}")
    public JournalEntryResponse getJournalEntryByTransaction(@PathVariable String transactionUuid) {
        return service.obtenerAsientoPorTransaccion(transactionUuid);
    }

    @GetMapping("/journal-entries/{journalEntryUuid}")
    public JournalEntryResponse getJournalEntry(@PathVariable String journalEntryUuid) { return service.obtenerAsiento(journalEntryUuid); }

    @PostMapping("/journal-entries/{journalEntryUuid}/reverse")
    @ResponseStatus(HttpStatus.CREATED)
    public JournalEntryResponse reverseJournalEntry(@PathVariable String journalEntryUuid) { return service.reversarAsiento(journalEntryUuid); }

    @PostMapping("/eod/run")
    @ResponseStatus(HttpStatus.CREATED)
    public EodResponse runEod(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accountingDate,
            @RequestParam(defaultValue = "false") boolean windowOverride,
            @RequestParam(required = false) String overrideReason,
            Authentication authentication) {
        boolean adminAuthorized = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN_SEGURIDAD"
                        .equals(authority.getAuthority())
                        || "ROLE_ADMIN_CORE".equals(authority.getAuthority()));
        return service.ejecutarEod(
                accountingDate,
                windowOverride,
                overrideReason,
                adminAuthorized,
                authentication == null ? null : authentication.getName());
    }

    @GetMapping("/eod/{eodUuid}")
    public EodResponse getEod(@PathVariable String eodUuid) { return service.obtenerEod(eodUuid); }

    @GetMapping("/eod/{eodUuid}/status")
    public EodResponse getEodStatus(@PathVariable String eodUuid) { return service.obtenerEod(eodUuid); }

    @GetMapping("/eod/by-date/{accountingDate}")
    public EodResponse getEodByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accountingDate) { return service.obtenerUltimoEodPorFecha(accountingDate); }

    @GetMapping("/trial-balances/{accountingDate}")
    public TrialBalanceResponse getTrialBalance(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accountingDate) { return service.obtenerBalance(accountingDate); }

    @GetMapping(value = "/trial-balances/{accountingDate}/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportTrialBalance(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accountingDate) {
        TrialBalanceCsvResponse report = service.exportarBalance(accountingDate);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(report.filename()).build().toString())
                .body(report.content());
    }
}
