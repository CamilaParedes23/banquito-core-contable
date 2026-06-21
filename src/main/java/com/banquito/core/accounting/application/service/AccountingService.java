package com.banquito.core.accounting.application.service;

import com.banquito.core.accounting.api.dto.api.*;
import com.banquito.core.accounting.domain.enums.*;
import com.banquito.core.accounting.domain.model.*;
import com.banquito.core.accounting.domain.repository.*;
import com.banquito.core.accounting.shared.exception.BusinessException;
import com.banquito.core.accounting.shared.tracing.CorrelationIdHolder;
import com.banquito.core.accounting.shared.util.JsonUtil;
import com.banquito.core.accounting.infrastructure.grpc.client.AdminCalendarGrpcClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AccountingService {
    private final CuentaContableRepository cuentaRepository;
    private final CuentaInstitucionalRepository institucionalRepository;
    private final JornadaContableRepository jornadaRepository;
    private final AsientoContableRepository asientoRepository;
    private final DetalleAsientoContableRepository detalleAsientoRepository;
    private final ProcesoEodRepository eodRepository;
    private final BalanceComprobacionRepository balanceRepository;
    private final AuditoriaAccountingService auditoriaService;
    private final OutboxEventService outboxEventService;
    private final TrialBalanceCsvService trialBalanceCsvService;
    private final AdminCalendarGrpcClient adminCalendarGrpcClient;

    @Value("${banquito.accounting.zone-id:America/Guayaquil}")
    private String accountingZoneId;

    @Value("${banquito.accounting.eod-window-code:CORE_CONTABLE}")
    private String eodWindowCode;

    @Value("${banquito.accounting.eod-manual-override-enabled:true}")
    private boolean eodManualOverrideEnabled;

    @Transactional(readOnly = true)
    public List<ChartAccountResponse> listarPlanCuentas() {
        return cuentaRepository.findByEstadoOrderByCodigoContableAsc(EstadoCuentaContableEnum.ACTIVA).stream().map(AccountingMapper::toChartAccount).toList();
    }

    @Transactional(readOnly = true)
    public ChartAccountResponse obtenerCuentaContable(String code) { return AccountingMapper.toChartAccount(getCuentaContable(code)); }

    @Transactional(readOnly = true)
    public InstitutionalAccountResponse obtenerCuentaInstitucional(String functionalCode) {
        return AccountingMapper.toInstitutional(institucionalRepository.findByCodigoFuncional(functionalCode).orElseThrow(() -> new BusinessException("ACCOUNTING_INSTITUTIONAL_ACCOUNT_NOT_FOUND", "Cuenta institucional no encontrada", HttpStatus.NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public AccountingDateResponse obtenerFechaContableActual() { return AccountingMapper.toAccountingDate(getJornadaAbierta()); }

    @Transactional(readOnly = true)
    public AccountingDailyStatusResponse obtenerEstadoDiario(LocalDate date) {
        JornadaContable jornada = jornadaRepository.findByFechaContable(date).orElse(null);
        JornadaContable jornadaAbierta = jornadaRepository
                .findFirstByEstadoOrderByFechaContableDesc(EstadoJornadaContableEnum.ABIERTA)
                .orElse(null);
        ProcesoEod procesoEod = eodRepository
                .findTopByFechaContableOrderByFechaInicioDesc(date)
                .orElse(null);
        BalanceComprobacion balance = balanceRepository
                .findTopByFechaContableOrderByFechaGeneracionDesc(date)
                .orElse(null);
        AdminCalendarGrpcClient.BusinessDayInfo businessDay =
                adminCalendarGrpcClient.obtenerInformacionDia(date);
        EodExecutionWindowResponse executionWindow = evaluarVentanaEod(date, jornada, jornadaAbierta);

        String dayType = businessDay.holiday()
                ? "HOLIDAY"
                : businessDay.weekend()
                ? "WEEKEND"
                : businessDay.businessDay()
                ? "BUSINESS_DAY"
                : "NON_BUSINESS_DAY";

        return new AccountingDailyStatusResponse(
                date,
                jornada != null,
                jornada != null
                        && jornadaAbierta != null
                        && Objects.equals(jornadaAbierta.getId(), jornada.getId()),
                jornada == null ? "NO_REGISTRADA" : jornada.getEstado().name(),
                businessDay.businessDay(),
                dayType,
                businessDay.description(),
                procesoEod != null,
                procesoEod == null ? null : AccountingMapper.toEod(procesoEod),
                balance != null,
                balance == null ? null : AccountingMapper.toTrialBalance(balance),
                executionWindow);
    }

    @Transactional(readOnly = true)
    public LocalDate resolverFechaContableOperacion() {
        JornadaContable jornada = getJornadaAbierta();
        return resolverFechaContableOperacion(jornada, LocalDateTime.now());
    }

    @Transactional
    public AccountingDateResponse abrirFechaContable(LocalDate date, OpenAccountingDateRequest request) {
        jornadaRepository.findByFechaContable(date).ifPresent(j -> { throw new BusinessException("ACCOUNTING_DATE_ALREADY_EXISTS", "La fecha contable ya existe", HttpStatus.CONFLICT); });
        jornadaRepository.findFirstByEstadoOrderByFechaContableDesc(EstadoJornadaContableEnum.ABIERTA).ifPresent(j -> { throw new BusinessException("ACCOUNTING_DATE_ALREADY_OPEN", "Ya existe una jornada contable abierta", HttpStatus.CONFLICT); });
        LocalTime cutoff = request != null && request.cutoffTime() != null ? request.cutoffTime() : LocalTime.of(20, 0);
        String obs = request != null ? request.observation() : "Apertura manual";
        JornadaContable jornada = jornadaRepository.save(JornadaContable.abrir(date, cutoff, obs));
        auditoriaService.registrar(CorrelationIdHolder.get(), "OPEN_ACCOUNTING_DATE", "JORNADA_CONTABLE", date.toString(), ResultadoAuditoriaAccountingEnum.OK, null);
        return AccountingMapper.toAccountingDate(jornada);
    }

    @Transactional
    public AccountingDateResponse cerrarFechaContable(LocalDate date) {
        JornadaContable jornada = jornadaRepository.findByFechaContable(date)
                .orElseThrow(() -> new BusinessException("ACCOUNTING_DATE_NOT_FOUND", "Fecha contable no encontrada", HttpStatus.NOT_FOUND));
        if (jornada.getEstado() == EstadoJornadaContableEnum.CERRADA) {
            return AccountingMapper.toAccountingDate(jornada);
        }
        ProcesoEod eod = eodRepository.findTopByFechaContableOrderByFechaInicioDesc(date)
                .filter(process -> process.getEstado() == EstadoProcesoEodEnum.EXITOSO)
                .orElseThrow(() -> new BusinessException(
                        "ACCOUNTING_EOD_REQUIRED_TO_CLOSE_DATE",
                        "La fecha contable solo puede cerrarse después de un EOD exitoso",
                        HttpStatus.CONFLICT));
        jornada.cerrar("Cierre confirmado por EOD " + eod.getUuidEod());
        auditoriaService.registrar(CorrelationIdHolder.get(), "CLOSE_ACCOUNTING_DATE", "JORNADA_CONTABLE",
                date.toString(), ResultadoAuditoriaAccountingEnum.OK, null);
        return AccountingMapper.toAccountingDate(jornadaRepository.save(jornada));
    }

    @Transactional
    public JournalEntryResponse crearAsiento(JournalEntryRequest request) {
        if (request.lines() == null || request.lines().size() < 2) {
            throw new BusinessException(
                    "ACCOUNTING_JOURNAL_LINES_REQUIRED",
                    "El asiento debe tener al menos dos líneas",
                    HttpStatus.BAD_REQUEST);
        }
        if (request.correlationId() != null && !request.correlationId().isBlank()
                && request.operationType() != null && !request.operationType().isBlank()) {
            var existing = asientoRepository.findFirstByUuidCorrelacionAndTipoOperacionAndEstadoOrderByTimestampRegistroAsc(
                    request.correlationId(), request.operationType(), EstadoAsientoContableEnum.REGISTRADO);
            if (existing.isPresent()) {
                validarReplayAsiento(existing.get(), request);
                return AccountingMapper.toJournalEntry(existing.get());
            }
        }
        validarFechaContableOperable(request.accountingDate());
        ContextoOrigenAsientoEnum contexto = parseEnum(ContextoOrigenAsientoEnum.class, request.originContext(), "ACCOUNTING_INVALID_ORIGIN_CONTEXT");
        BigDecimal totalDebitos = BigDecimal.ZERO;
        BigDecimal totalCreditos = BigDecimal.ZERO;
        AsientoContable asiento = AsientoContable.crear(request.correlationId(), request.transactionUuid(), contexto, request.operationType(), request.description(), request.accountingDate(), request.externalReference());
        int order = 1;
        for (JournalEntryLineRequest line : request.lines()) {
            TipoMovimientoContableEnum tipo = parseEnum(TipoMovimientoContableEnum.class, line.movementType(), "ACCOUNTING_INVALID_MOVEMENT_TYPE");
            CuentaContable cuenta = resolveCuenta(line);
            if (!cuenta.esDetalleActivaMovible()) throw new BusinessException("ACCOUNTING_ACCOUNT_NOT_MOVABLE", "La cuenta contable no permite movimientos: " + cuenta.getCodigoContable(), HttpStatus.CONFLICT);
            BigDecimal amount = line.amount();
            if (tipo == TipoMovimientoContableEnum.DEBITO) totalDebitos = totalDebitos.add(amount); else totalCreditos = totalCreditos.add(amount);
            asiento.agregarDetalle(DetalleAsientoContable.crear(cuenta, tipo, amount, line.reference(), line.lineOrder() != null ? line.lineOrder() : order++));
        }
        if (totalDebitos.compareTo(totalCreditos) != 0) throw new BusinessException("ACCOUNTING_JOURNAL_NOT_BALANCED", "El asiento no cumple suma cero", HttpStatus.BAD_REQUEST);
        AsientoContable saved = asientoRepository.save(asiento);
        saved.getDetalles().forEach(d -> {
            d.getCuentaContable().aplicarMovimiento(d.getTipoMovimiento(), d.getMonto());
            cuentaRepository.save(d.getCuentaContable());
        });
        outboxEventService.registrar(saved.getUuidCorrelacion(), "JOURNAL_ENTRY_REGISTERED", "ASIENTO_CONTABLE", saved.getUuidAsiento(), "{\"journalEntryUuid\":\"" + saved.getUuidAsiento() + "\"}");
        auditoriaService.registrar(saved.getUuidCorrelacion(), "CREATE_JOURNAL_ENTRY", "ASIENTO_CONTABLE", saved.getUuidAsiento(), ResultadoAuditoriaAccountingEnum.OK, null);
        return AccountingMapper.toJournalEntry(saved);
    }

    @Transactional(readOnly = true)
    public JournalEntryListResponse listarAsientos(
            LocalDate dateFrom,
            LocalDate dateTo,
            String operationType,
            String status,
            String journalEntryUuid,
            String transactionUuid,
            String correlationId,
            int page,
            int size) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessException(
                    "ACCOUNTING_INVALID_DATE_RANGE",
                    "La fecha inicial no puede ser posterior a la fecha final",
                    HttpStatus.BAD_REQUEST);
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        EstadoAsientoContableEnum normalizedStatus = parseOptionalEnum(
                EstadoAsientoContableEnum.class,
                status,
                "ACCOUNTING_INVALID_JOURNAL_STATUS");
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<AsientoContable> result = asientoRepository.searchJournalEntries(
                dateFrom,
                dateTo,
                normalize(operationType),
                normalizedStatus,
                normalize(journalEntryUuid),
                normalize(transactionUuid),
                normalize(correlationId),
                pageable);

        List<Long> entryIds = result.getContent().stream()
                .map(AsientoContable::getId)
                .toList();
        Map<Long, List<DetalleAsientoContable>> linesByEntry = new HashMap<>();
        if (!entryIds.isEmpty()) {
            detalleAsientoRepository
                    .findByAsientoContableIdInOrderByAsientoContableIdAscOrdenLineaAsc(entryIds)
                    .forEach(line -> linesByEntry
                            .computeIfAbsent(line.getAsientoContable().getId(), ignored -> new ArrayList<>())
                            .add(line));
        }

        List<JournalEntrySummaryResponse> entries = result.getContent().stream()
                .map(entry -> AccountingMapper.toJournalEntrySummary(
                        entry,
                        linesByEntry.getOrDefault(entry.getId(), List.of())))
                .toList();
        return new JournalEntryListResponse(
                result.getTotalElements(),
                result.getNumber(),
                result.getSize(),
                result.getTotalPages(),
                entries);
    }

    @Transactional(readOnly = true)
    public JournalEntryResponse obtenerAsiento(String uuid) { return AccountingMapper.toJournalEntry(getAsiento(uuid)); }

    @Transactional(readOnly = true)
    public JournalEntryResponse obtenerAsientoPorTransaccion(String transactionUuid) {
        String normalized = normalize(transactionUuid);
        if (normalized == null) {
            throw new BusinessException("ACCOUNTING_TRANSACTION_UUID_REQUIRED", "El UUID de transacción es obligatorio", HttpStatus.BAD_REQUEST);
        }
        AsientoContable asiento = asientoRepository
                .findFirstByTransaccionUuidAndEstadoOrderByTimestampRegistroAsc(normalized, EstadoAsientoContableEnum.REGISTRADO)
                .orElseThrow(() -> new BusinessException("ACCOUNTING_JOURNAL_BY_TRANSACTION_NOT_FOUND", "No existe asiento para la transacción indicada", HttpStatus.NOT_FOUND));
        return AccountingMapper.toJournalEntry(asiento);
    }

    @Transactional
    public JournalEntryResponse reversarAsiento(String uuid) {
        AsientoContable original = getAsiento(uuid);
        if (original.getEstado() != EstadoAsientoContableEnum.REGISTRADO) throw new BusinessException("ACCOUNTING_JOURNAL_NOT_REVERSIBLE", "El asiento no puede ser reversado", HttpStatus.CONFLICT);
        AsientoContable reverso = AsientoContable.crear(original.getUuidCorrelacion(), original.getTransaccionUuid(), ContextoOrigenAsientoEnum.CORE_INTERNO, "REVERSO_" + original.getTipoOperacion(), "Reverso de asiento " + original.getUuidAsiento(), original.getFechaContable(), original.getUuidAsiento());
        int order = 1;
        for (DetalleAsientoContable d : original.getDetalles()) {
            TipoMovimientoContableEnum tipoReverso = d.getTipoMovimiento() == TipoMovimientoContableEnum.DEBITO ? TipoMovimientoContableEnum.CREDITO : TipoMovimientoContableEnum.DEBITO;
            reverso.agregarDetalle(DetalleAsientoContable.crear(d.getCuentaContable(), tipoReverso, d.getMonto(), "Reverso " + d.getReferencia(), order++));
        }
        AsientoContable saved = asientoRepository.save(reverso);
        saved.getDetalles().forEach(d -> { d.getCuentaContable().aplicarMovimiento(d.getTipoMovimiento(), d.getMonto()); cuentaRepository.save(d.getCuentaContable()); });
        original.marcarReversado();
        asientoRepository.save(original);
        outboxEventService.registrar(saved.getUuidCorrelacion(), "JOURNAL_ENTRY_REVERSED", "ASIENTO_CONTABLE", saved.getUuidAsiento(), "{\"originalJournalEntryUuid\":\"" + JsonUtil.escape(original.getUuidAsiento()) + "\",\"reversalJournalEntryUuid\":\"" + JsonUtil.escape(saved.getUuidAsiento()) + "\"}");
        return AccountingMapper.toJournalEntry(saved);
    }

    @Transactional
    public EodResponse ejecutarEod(LocalDate date) {
        return ejecutarEod(date, false, null, false, null);
    }

    @Transactional
    public EodResponse ejecutarEod(
            LocalDate date,
            boolean windowOverride,
            String overrideReason,
            boolean adminAuthorized,
            String actorSubject) {
        JornadaContable jornada = jornadaRepository.findByFechaContableForUpdate(date)
                .orElseThrow(() -> new BusinessException(
                        "ACCOUNTING_DATE_NOT_FOUND",
                        "Fecha contable no encontrada",
                        HttpStatus.NOT_FOUND));

        if (jornada.getEstado() == EstadoJornadaContableEnum.CERRADA) {
            ProcesoEod existingEod = eodRepository
                    .findTopByFechaContableOrderByFechaInicioDesc(date)
                    .filter(process -> process.getEstado() == EstadoProcesoEodEnum.EXITOSO)
                    .orElseThrow(() -> new BusinessException(
                            "ACCOUNTING_DATE_NOT_OPEN",
                            "La fecha contable no está abierta",
                            HttpStatus.CONFLICT));

            JornadaContable currentOpenDate = jornadaRepository
                    .findFirstByEstadoOrderByFechaContableDesc(
                            EstadoJornadaContableEnum.ABIERTA)
                    .orElse(null);

            return AccountingMapper.toEod(existingEod, currentOpenDate);
        }

        if (jornada.getEstado() != EstadoJornadaContableEnum.ABIERTA) {
            throw new BusinessException(
                    "ACCOUNTING_DATE_NOT_OPEN",
                    "La fecha contable no está abierta",
                    HttpStatus.CONFLICT);
        }

        JornadaContable currentOpenDate = jornadaRepository
                .findFirstByEstadoOrderByFechaContableDesc(
                        EstadoJornadaContableEnum.ABIERTA)
                .orElse(null);
        if (currentOpenDate != null && !currentOpenDate.getId().equals(jornada.getId())) {
            throw new BusinessException(
                    "ACCOUNTING_MULTIPLE_OPEN_DATES",
                    "Existe otra jornada contable abierta",
                    HttpStatus.CONFLICT);
        }

        EodExecutionWindowResponse executionWindow = evaluarVentanaEod(
                date,
                jornada,
                currentOpenDate);
        boolean overrideUsed = validarAutorizacionVentanaEod(
                executionWindow,
                windowOverride,
                overrideReason,
                adminAuthorized);

        LocalDate nextBusinessDate =
                adminCalendarGrpcClient.obtenerSiguienteDiaHabil(date);

        if (jornadaRepository.findByFechaContable(nextBusinessDate).isPresent()) {
            throw new BusinessException(
                    "ACCOUNTING_NEXT_DATE_ALREADY_EXISTS",
                    "La siguiente fecha contable ya existe: "
                            + nextBusinessDate,
                    HttpStatus.CONFLICT);
        }

        List<AsientoContable> asientos = asientoRepository
                .findByFechaContableAndEstadoOrderByTimestampRegistroAsc(
                        date, EstadoAsientoContableEnum.REGISTRADO);
        BigDecimal debitos = BigDecimal.ZERO;
        BigDecimal creditos = BigDecimal.ZERO;

        for (AsientoContable asiento : asientos) {
            for (DetalleAsientoContable detalle : asiento.getDetalles()) {
                if (detalle.getTipoMovimiento()
                        == TipoMovimientoContableEnum.DEBITO) {
                    debitos = debitos.add(detalle.getMonto());
                } else {
                    creditos = creditos.add(detalle.getMonto());
                }
            }
        }

        if (debitos.compareTo(creditos) != 0) {
            throw new BusinessException(
                    "ACCOUNTING_EOD_JOURNALS_NOT_BALANCED",
                    "El EOD no puede finalizar porque los débitos y créditos "
                            + "de la jornada no cuadran",
                    HttpStatus.CONFLICT);
        }

        validarSaldosPlanCuentasCuadrados();
        jornada.iniciarCierre();

        ProcesoEod eod = eodRepository.save(ProcesoEod.iniciar(jornada));
        BalanceComprobacion balance = generarBalance(eod);

        if (balance.getEstado()
                != EstadoBalanceComprobacionEnum.CUADRADO) {
            throw new BusinessException(
                    "ACCOUNTING_TRIAL_BALANCE_NOT_BALANCED",
                    "El EOD no puede finalizar porque el balance de "
                            + "comprobación está descuadrado",
                    HttpStatus.CONFLICT);
        }

        String reportUuid = trialBalanceCsvService.write(balance);
        balance.setUuidDocumentoCsv(reportUuid);
        balanceRepository.save(balance);

        eod.setUuidDocumentoReporte(reportUuid);
        eod.exitoso(debitos, creditos);
        jornada.cerrar("Cierre EOD exitoso. Balance CUADRADO");
        jornadaRepository.save(jornada);

        JornadaContable nextAccountingDate = jornadaRepository.save(
                JornadaContable.abrir(
                        nextBusinessDate,
                        jornada.getHoraCorte(),
                        "Apertura automática posterior al EOD "
                                + eod.getUuidEod()));

        outboxEventService.registrar(
                CorrelationIdHolder.get(),
                "EOD_COMPLETED",
                "PROCESO_EOD",
                eod.getUuidEod(),
                "{\"eodUuid\":\"" + eod.getUuidEod()
                        + "\",\"accountingDate\":\"" + date
                        + "\",\"nextAccountingDate\":\""
                        + nextBusinessDate
                        + "\",\"reportDocumentUuid\":\""
                        + reportUuid + "\"}");

        if (overrideUsed) {
            auditoriaService.registrar(
                    CorrelationIdHolder.get(),
                    "RUN_EOD_WINDOW_OVERRIDE",
                    "PROCESO_EOD",
                    eod.getUuidEod(),
                    ResultadoAuditoriaAccountingEnum.OK,
                    "{\"accountingDate\":\"" + date
                            + "\",\"restrictionCode\":\""
                            + JsonUtil.escape(executionWindow.restrictionCode())
                            + "\",\"reason\":\""
                            + JsonUtil.escape(overrideReason.trim())
                            + "\",\"actorSubject\":\""
                            + JsonUtil.escape(actorSubject) + "\"}");
        }

        auditoriaService.registrar(
                CorrelationIdHolder.get(),
                "RUN_EOD",
                "PROCESO_EOD",
                eod.getUuidEod(),
                ResultadoAuditoriaAccountingEnum.OK,
                "{\"closedAccountingDate\":\"" + date
                        + "\",\"nextAccountingDate\":\""
                        + nextBusinessDate
                        + "\",\"windowOverride\":" + overrideUsed + "}");

        auditoriaService.registrar(
                CorrelationIdHolder.get(),
                "OPEN_ACCOUNTING_DATE",
                "JORNADA_CONTABLE",
                nextBusinessDate.toString(),
                ResultadoAuditoriaAccountingEnum.OK,
                "{\"source\":\"EOD\",\"eodUuid\":\""
                        + eod.getUuidEod() + "\"}");

        return AccountingMapper.toEod(
                eodRepository.save(eod),
                nextAccountingDate);
    }

    @Transactional
    public AccountingDateResponse recuperarSiguienteFechaContable() {
        var currentOpenDate = jornadaRepository
                .findFirstByEstadoOrderByFechaContableDesc(
                        EstadoJornadaContableEnum.ABIERTA);

        if (currentOpenDate.isPresent()) {
            return AccountingMapper.toAccountingDate(currentOpenDate.get());
        }

        JornadaContable latestDate = jornadaRepository
                .findFirstByOrderByFechaContableDesc()
                .orElseThrow(() -> new BusinessException(
                        "ACCOUNTING_DATE_HISTORY_NOT_FOUND",
                        "No existe una jornada contable previa para recuperar",
                        HttpStatus.CONFLICT));

        LocalDate nextBusinessDate =
                adminCalendarGrpcClient.obtenerSiguienteDiaHabil(
                        latestDate.getFechaContable());

        if (jornadaRepository.findByFechaContable(nextBusinessDate).isPresent()) {
            throw new BusinessException(
                    "ACCOUNTING_NEXT_DATE_ALREADY_EXISTS",
                    "La siguiente fecha contable ya existe: "
                            + nextBusinessDate,
                    HttpStatus.CONFLICT);
        }

        JornadaContable recoveredDate = jornadaRepository.save(
                JornadaContable.abrir(
                        nextBusinessDate,
                        latestDate.getHoraCorte(),
                        "Apertura de contingencia posterior a la jornada "
                                + latestDate.getFechaContable()));

        auditoriaService.registrar(
                CorrelationIdHolder.get(),
                "RECOVER_NEXT_ACCOUNTING_DATE",
                "JORNADA_CONTABLE",
                nextBusinessDate.toString(),
                ResultadoAuditoriaAccountingEnum.OK,
                "{\"previousAccountingDate\":\""
                        + latestDate.getFechaContable()
                        + "\",\"nextAccountingDate\":\""
                        + nextBusinessDate + "\"}");

        return AccountingMapper.toAccountingDate(recoveredDate);
    }

    @Transactional(readOnly = true)
    public EodResponse obtenerEod(String uuid) { return AccountingMapper.toEod(eodRepository.findByUuidEod(uuid).orElseThrow(() -> new BusinessException("ACCOUNTING_EOD_NOT_FOUND", "Proceso EOD no encontrado", HttpStatus.NOT_FOUND))); }

    @Transactional(readOnly = true)
    public EodResponse obtenerUltimoEodPorFecha(LocalDate date) { return AccountingMapper.toEod(eodRepository.findTopByFechaContableOrderByFechaInicioDesc(date).orElseThrow(() -> new BusinessException("ACCOUNTING_EOD_NOT_FOUND", "No existe EOD para la fecha", HttpStatus.NOT_FOUND))); }

    @Transactional(readOnly = true)
    public TrialBalanceResponse obtenerBalance(LocalDate date) { return AccountingMapper.toTrialBalance(balanceRepository.findTopByFechaContableOrderByFechaGeneracionDesc(date).orElseThrow(() -> new BusinessException("ACCOUNTING_TRIAL_BALANCE_NOT_FOUND", "Balance de comprobación no encontrado", HttpStatus.NOT_FOUND))); }


    @Transactional(readOnly = true)
    public TrialBalanceCsvResponse exportarBalance(LocalDate date) {
        BalanceComprobacion balance = balanceRepository.findTopByFechaContableOrderByFechaGeneracionDesc(date)
                .orElseThrow(() -> new BusinessException("ACCOUNTING_TRIAL_BALANCE_NOT_FOUND", "Balance de comprobación no encontrado", HttpStatus.NOT_FOUND));
        if (balance.getEstado() != EstadoBalanceComprobacionEnum.CUADRADO
                || balance.getUuidDocumentoCsv() == null
                || balance.getUuidDocumentoCsv().isBlank()) {
            throw new BusinessException(
                    "ACCOUNTING_TRIAL_BALANCE_EXPORT_NOT_AVAILABLE",
                    "El balance no tiene un reporte CSV válido disponible",
                    HttpStatus.CONFLICT);
        }
        return new TrialBalanceCsvResponse(
                "balance-comprobacion-" + date + ".csv",
                trialBalanceCsvService.read(balance.getUuidDocumentoCsv()));
    }

    private EodExecutionWindowResponse evaluarVentanaEod(
            LocalDate accountingDate,
            JornadaContable jornada,
            JornadaContable jornadaAbierta) {
        AdminCalendarGrpcClient.OperationalWindowInfo window =
                adminCalendarGrpcClient.obtenerVentanaOperativa(eodWindowCode);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(accountingZoneId));
        LocalDate serverDate = now.toLocalDate();
        LocalTime serverTime = now.toLocalTime().withNano(0);

        String restrictionCode = null;
        String restrictionMessage = null;

        if (!"ACTIVA".equalsIgnoreCase(window.status())) {
            restrictionCode = "ACCOUNTING_EOD_WINDOW_INACTIVE";
            restrictionMessage = "La ventana operativa contable se encuentra inactiva.";
        } else if (jornada == null) {
            restrictionCode = "ACCOUNTING_DATE_NOT_FOUND";
            restrictionMessage = "La fecha seleccionada no corresponde a una jornada contable registrada.";
        } else if (jornada.getEstado() == EstadoJornadaContableEnum.CERRADA) {
            restrictionCode = "ACCOUNTING_DATE_ALREADY_CLOSED";
            restrictionMessage = "La jornada contable ya se encuentra cerrada.";
        } else if (jornada.getEstado() != EstadoJornadaContableEnum.ABIERTA) {
            restrictionCode = "ACCOUNTING_DATE_NOT_OPEN";
            restrictionMessage = "La jornada contable no se encuentra abierta.";
        } else if (jornadaAbierta == null || !Objects.equals(jornadaAbierta.getId(), jornada.getId())) {
            restrictionCode = "ACCOUNTING_DATE_NOT_ACTIVE";
            restrictionMessage = "El EOD solo puede ejecutarse sobre la jornada contable activa.";
        } else if (accountingDate.isAfter(serverDate)) {
            restrictionCode = "ACCOUNTING_EOD_FUTURE_DATE";
            restrictionMessage = "La fecha contable está adelantada respecto de la fecha física del servidor.";
        } else if (accountingDate.isBefore(serverDate)) {
            restrictionCode = "ACCOUNTING_EOD_RECOVERY_REQUIRED";
            restrictionMessage = "La fecha contable está atrasada y requiere cierre secuencial de recuperación.";
        } else if (serverTime.isBefore(window.cutoffTime())) {
            restrictionCode = "ACCOUNTING_EOD_BEFORE_CUTOFF";
            restrictionMessage = "El EOD normal está habilitado desde la hora de corte "
                    + window.cutoffTime() + ".";
        } else if (serverTime.isAfter(window.endTime())) {
            restrictionCode = "ACCOUNTING_EOD_AFTER_WINDOW";
            restrictionMessage = "La ventana operativa de EOD finalizó a las "
                    + window.endTime() + ".";
        }

        boolean normalAllowed = restrictionCode == null;
        boolean temporalRestriction = List.of(
                        "ACCOUNTING_EOD_FUTURE_DATE",
                        "ACCOUNTING_EOD_RECOVERY_REQUIRED",
                        "ACCOUNTING_EOD_BEFORE_CUTOFF",
                        "ACCOUNTING_EOD_AFTER_WINDOW")
                .contains(restrictionCode);
        boolean overrideRequired = temporalRestriction
                && jornada != null
                && jornada.getEstado() == EstadoJornadaContableEnum.ABIERTA
                && jornadaAbierta != null
                && Objects.equals(jornadaAbierta.getId(), jornada.getId());

        return new EodExecutionWindowResponse(
                window.code(),
                window.startTime(),
                window.cutoffTime(),
                window.endTime(),
                window.actionAfterCutoff(),
                serverDate,
                serverTime,
                normalAllowed,
                overrideRequired,
                overrideRequired && eodManualOverrideEnabled,
                restrictionCode,
                restrictionMessage);
    }

    private boolean validarAutorizacionVentanaEod(
            EodExecutionWindowResponse executionWindow,
            boolean windowOverride,
            String overrideReason,
            boolean adminAuthorized) {
        if (executionWindow.normalExecutionAllowed()) {
            return false;
        }

        if (!executionWindow.overrideRequired()) {
            throw new BusinessException(
                    executionWindow.restrictionCode(),
                    executionWindow.restrictionMessage(),
                    HttpStatus.CONFLICT);
        }

        if (!windowOverride) {
            throw new BusinessException(
                    executionWindow.restrictionCode(),
                    executionWindow.restrictionMessage()
                            + " Un administrador puede registrar una excepción justificada.",
                    HttpStatus.CONFLICT);
        }

        if (!eodManualOverrideEnabled) {
            throw new BusinessException(
                    "ACCOUNTING_EOD_OVERRIDE_DISABLED",
                    "Las excepciones manuales de ventana EOD están deshabilitadas.",
                    HttpStatus.CONFLICT);
        }

        if (!adminAuthorized) {
            throw new BusinessException(
                    "ACCOUNTING_EOD_OVERRIDE_FORBIDDEN",
                    "Solo un Administrador Core puede autorizar una excepción de ventana EOD.",
                    HttpStatus.FORBIDDEN);
        }

        String normalizedReason = normalize(overrideReason);
        if (normalizedReason == null || normalizedReason.length() < 10) {
            throw new BusinessException(
                    "ACCOUNTING_EOD_OVERRIDE_REASON_REQUIRED",
                    "La excepción EOD requiere un motivo de al menos 10 caracteres.",
                    HttpStatus.BAD_REQUEST);
        }
        if (normalizedReason.length() > 500) {
            throw new BusinessException(
                    "ACCOUNTING_EOD_OVERRIDE_REASON_TOO_LONG",
                    "El motivo de la excepción EOD no puede superar 500 caracteres.",
                    HttpStatus.BAD_REQUEST);
        }
        return true;
    }

    private void validarSaldosPlanCuentasCuadrados() {
        BigDecimal totalDeudor = BigDecimal.ZERO;
        BigDecimal totalAcreedor = BigDecimal.ZERO;
        for (CuentaContable cuenta : cuentaRepository.findByEstadoOrderByCodigoContableAsc(EstadoCuentaContableEnum.ACTIVA)) {
            if (cuenta.getTipoCuenta() != TipoCuentaContableEnum.DETALLE) {
                continue;
            }
            totalDeudor = totalDeudor.add(saldoDeudor(cuenta));
            totalAcreedor = totalAcreedor.add(saldoAcreedor(cuenta));
        }
        if (totalDeudor.compareTo(totalAcreedor) != 0) {
            throw new BusinessException(
                    "ACCOUNTING_TRIAL_BALANCE_NOT_BALANCED",
                    "El EOD no puede finalizar porque el plan de cuentas presenta saldos descuadrados",
                    HttpStatus.CONFLICT);
        }
    }

    private BalanceComprobacion generarBalance(ProcesoEod eod) {
        List<CuentaContable> cuentas = cuentaRepository.findByEstadoOrderByCodigoContableAsc(EstadoCuentaContableEnum.ACTIVA);
        BigDecimal totalDeudor = BigDecimal.ZERO;
        BigDecimal totalAcreedor = BigDecimal.ZERO;
        for (CuentaContable c : cuentas) {
            if (c.getTipoCuenta() != TipoCuentaContableEnum.DETALLE) continue;
            totalDeudor = totalDeudor.add(saldoDeudor(c));
            totalAcreedor = totalAcreedor.add(saldoAcreedor(c));
        }
        BalanceComprobacion balance = BalanceComprobacion.crear(eod, totalDeudor, totalAcreedor);
        int order = 1;
        for (CuentaContable c : cuentas) {
            if (c.getTipoCuenta() != TipoCuentaContableEnum.DETALLE) continue;
            balance.agregarDetalle(BalanceComprobacionDetalle.crear(
                    c, saldoDeudor(c), saldoAcreedor(c), order++));
        }
        return balanceRepository.save(balance);
    }

    private BigDecimal saldoDeudor(CuentaContable cuenta) {
        BigDecimal saldo = cuenta.getSaldoActual() == null ? BigDecimal.ZERO : cuenta.getSaldoActual();
        boolean ladoNatural = saldo.signum() >= 0;
        boolean esDeudor = ladoNatural
                ? cuenta.getNaturalezaSaldo() == NaturalezaSaldoEnum.DEUDORA
                : cuenta.getNaturalezaSaldo() == NaturalezaSaldoEnum.ACREEDORA;
        return esDeudor ? saldo.abs() : BigDecimal.ZERO;
    }

    private BigDecimal saldoAcreedor(CuentaContable cuenta) {
        BigDecimal saldo = cuenta.getSaldoActual() == null ? BigDecimal.ZERO : cuenta.getSaldoActual();
        boolean ladoNatural = saldo.signum() >= 0;
        boolean esAcreedor = ladoNatural
                ? cuenta.getNaturalezaSaldo() == NaturalezaSaldoEnum.ACREEDORA
                : cuenta.getNaturalezaSaldo() == NaturalezaSaldoEnum.DEUDORA;
        return esAcreedor ? saldo.abs() : BigDecimal.ZERO;
    }

    private CuentaContable resolveCuenta(JournalEntryLineRequest line) {
        if (line.accountingCode() != null && !line.accountingCode().isBlank()) return getCuentaContable(line.accountingCode());
        if (line.institutionalAccountCode() != null && !line.institutionalAccountCode().isBlank()) return institucionalRepository.findByCodigoFuncional(line.institutionalAccountCode()).orElseThrow(() -> new BusinessException("ACCOUNTING_INSTITUTIONAL_ACCOUNT_NOT_FOUND", "Cuenta institucional no encontrada", HttpStatus.NOT_FOUND)).getCuentaContable();
        throw new BusinessException("ACCOUNTING_LINE_ACCOUNT_REQUIRED", "Cada línea debe enviar accountingCode o institutionalAccountCode", HttpStatus.BAD_REQUEST);
    }

    private CuentaContable getCuentaContable(String code) { return cuentaRepository.findByCodigoContable(code).orElseThrow(() -> new BusinessException("ACCOUNTING_ACCOUNT_NOT_FOUND", "Cuenta contable no encontrada", HttpStatus.NOT_FOUND)); }
    private AsientoContable getAsiento(String uuid) { return asientoRepository.findByUuidAsiento(uuid).orElseThrow(() -> new BusinessException("ACCOUNTING_JOURNAL_NOT_FOUND", "Asiento contable no encontrado", HttpStatus.NOT_FOUND)); }
    private JornadaContable getJornadaAbierta() { return jornadaRepository.findFirstByEstadoOrderByFechaContableDesc(EstadoJornadaContableEnum.ABIERTA).orElseThrow(() -> new BusinessException("ACCOUNTING_DATE_NOT_OPEN", "No existe una fecha contable abierta", HttpStatus.NOT_FOUND)); }

    private LocalDate resolverFechaContableOperacion(JornadaContable jornada, LocalDateTime timestamp) {
        if (timestamp.toLocalTime().isBefore(jornada.getHoraCorte())) {
            return jornada.getFechaContable();
        }
        return adminCalendarGrpcClient.obtenerSiguienteDiaHabil(jornada.getFechaContable());
    }

    private void validarFechaContableOperable(LocalDate date) {
        JornadaContable jornada = getJornadaAbierta();
        LocalDate expectedDate = resolverFechaContableOperacion(jornada, LocalDateTime.now());
        if (!Objects.equals(expectedDate, date)) {
            throw new BusinessException(
                    "ACCOUNTING_OPERATION_DATE_MISMATCH",
                    "La fecha contable de la operación debe ser " + expectedDate
                            + (expectedDate.equals(jornada.getFechaContable())
                            ? " antes del horario de corte"
                            : " por haberse superado el horario de corte"),
                    HttpStatus.CONFLICT);
        }
    }

    private void validarReplayAsiento(AsientoContable existing, JournalEntryRequest request) {
        ContextoOrigenAsientoEnum requestedContext = parseEnum(
                ContextoOrigenAsientoEnum.class,
                request.originContext(),
                "ACCOUNTING_INVALID_ORIGIN_CONTEXT");
        boolean headerMatches = Objects.equals(existing.getTransaccionUuid(), request.transactionUuid())
                && existing.getContextoOrigen() == requestedContext
                && Objects.equals(existing.getFechaContable(), request.accountingDate())
                && Objects.equals(normalize(existing.getReferenciaExterna()), normalize(request.externalReference()));
        if (!headerMatches || existing.getDetalles().size() != request.lines().size()) {
            throw idempotencyPayloadConflict();
        }

        List<DetalleAsientoContable> existingLines = existing.getDetalles().stream()
                .sorted(Comparator.comparing(DetalleAsientoContable::getOrdenLinea))
                .toList();
        List<JournalEntryLineRequest> requestedLines = request.lines().stream()
                .sorted(Comparator.comparing(line -> line.lineOrder() == null ? Integer.MAX_VALUE : line.lineOrder()))
                .toList();
        for (int index = 0; index < requestedLines.size(); index++) {
            JournalEntryLineRequest requested = requestedLines.get(index);
            DetalleAsientoContable persisted = existingLines.get(index);
            TipoMovimientoContableEnum requestedMovement = parseEnum(
                    TipoMovimientoContableEnum.class,
                    requested.movementType(),
                    "ACCOUNTING_INVALID_MOVEMENT_TYPE");
            CuentaContable requestedAccount = resolveCuenta(requested);
            Integer requestedOrder = requested.lineOrder() == null ? index + 1 : requested.lineOrder();
            boolean lineMatches = Objects.equals(persisted.getCuentaContable().getId(), requestedAccount.getId())
                    && persisted.getTipoMovimiento() == requestedMovement
                    && persisted.getMonto().compareTo(requested.amount()) == 0
                    && Objects.equals(persisted.getOrdenLinea(), requestedOrder);
            if (!lineMatches) throw idempotencyPayloadConflict();
        }
    }

    private BusinessException idempotencyPayloadConflict() {
        return new BusinessException(
                "ACCOUNTING_IDEMPOTENCY_PAYLOAD_CONFLICT",
                "La correlación contable ya fue utilizada con un asiento diferente",
                HttpStatus.CONFLICT);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private <E extends Enum<E>> E parseOptionalEnum(Class<E> type, String value, String code) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseEnum(type, value.trim().toUpperCase(Locale.ROOT), code);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String code) {
        try {
            return Enum.valueOf(type, value);
        } catch (Exception ex) {
            throw new BusinessException(code, "Valor inválido: " + value, HttpStatus.BAD_REQUEST);
        }
    }
}
