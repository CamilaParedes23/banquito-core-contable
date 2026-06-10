package com.banquito.core.accounting.application.service;

import com.banquito.core.accounting.api.dto.api.*;
import com.banquito.core.accounting.domain.enums.*;
import com.banquito.core.accounting.domain.model.*;
import com.banquito.core.accounting.domain.repository.*;
import com.banquito.core.accounting.shared.exception.BusinessException;
import com.banquito.core.accounting.shared.tracing.CorrelationIdHolder;
import com.banquito.core.accounting.shared.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountingService {
    private final CuentaContableRepository cuentaRepository;
    private final CuentaInstitucionalRepository institucionalRepository;
    private final JornadaContableRepository jornadaRepository;
    private final AsientoContableRepository asientoRepository;
    private final ProcesoEodRepository eodRepository;
    private final BalanceComprobacionRepository balanceRepository;
    private final AuditoriaAccountingService auditoriaService;
    private final OutboxEventService outboxEventService;

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
        JornadaContable jornada = jornadaRepository.findByFechaContable(date).orElseThrow(() -> new BusinessException("ACCOUNTING_DATE_NOT_FOUND", "Fecha contable no encontrada", HttpStatus.NOT_FOUND));
        if (jornada.getEstado() == EstadoJornadaContableEnum.CERRADA) return AccountingMapper.toAccountingDate(jornada);
        jornada.cerrar("Cierre manual");
        auditoriaService.registrar(CorrelationIdHolder.get(), "CLOSE_ACCOUNTING_DATE", "JORNADA_CONTABLE", date.toString(), ResultadoAuditoriaAccountingEnum.OK, null);
        return AccountingMapper.toAccountingDate(jornadaRepository.save(jornada));
    }

    @Transactional
    public JournalEntryResponse crearAsiento(JournalEntryRequest request) {
        validarFechaContableAbierta(request.accountingDate());
        if (request.lines() == null || request.lines().size() < 2) throw new BusinessException("ACCOUNTING_JOURNAL_LINES_REQUIRED", "El asiento debe tener al menos dos líneas", HttpStatus.BAD_REQUEST);
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
    public JournalEntryResponse obtenerAsiento(String uuid) { return AccountingMapper.toJournalEntry(getAsiento(uuid)); }

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
        JornadaContable jornada = jornadaRepository.findByFechaContable(date).orElseThrow(() -> new BusinessException("ACCOUNTING_DATE_NOT_FOUND", "Fecha contable no encontrada", HttpStatus.NOT_FOUND));
        if (jornada.getEstado() != EstadoJornadaContableEnum.ABIERTA) throw new BusinessException("ACCOUNTING_DATE_NOT_OPEN", "La fecha contable no está abierta", HttpStatus.CONFLICT);
        jornada.iniciarCierre();
        ProcesoEod eod = eodRepository.save(ProcesoEod.iniciar(jornada));
        try {
            List<AsientoContable> asientos = asientoRepository.findByFechaContableAndEstadoOrderByTimestampRegistroAsc(date, EstadoAsientoContableEnum.REGISTRADO);
            BigDecimal debitos = BigDecimal.ZERO;
            BigDecimal creditos = BigDecimal.ZERO;
            for (AsientoContable asiento : asientos) {
                for (DetalleAsientoContable d : asiento.getDetalles()) {
                    if (d.getTipoMovimiento() == TipoMovimientoContableEnum.DEBITO) debitos = debitos.add(d.getMonto()); else creditos = creditos.add(d.getMonto());
                }
            }
            BalanceComprobacion balance = generarBalance(eod);
            eod.exitoso(debitos, creditos);
            jornada.cerrar("Cierre EOD exitoso. Balance " + balance.getEstado());
            outboxEventService.registrar(CorrelationIdHolder.get(), "EOD_COMPLETED", "PROCESO_EOD", eod.getUuidEod(), "{\"eodUuid\":\"" + eod.getUuidEod() + "\",\"accountingDate\":\"" + date + "\"}");
            return AccountingMapper.toEod(eodRepository.save(eod));
        } catch (RuntimeException ex) {
            eod.fallido(ex.getMessage());
            eodRepository.save(eod);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public EodResponse obtenerEod(String uuid) { return AccountingMapper.toEod(eodRepository.findByUuidEod(uuid).orElseThrow(() -> new BusinessException("ACCOUNTING_EOD_NOT_FOUND", "Proceso EOD no encontrado", HttpStatus.NOT_FOUND))); }

    @Transactional(readOnly = true)
    public EodResponse obtenerUltimoEodPorFecha(LocalDate date) { return AccountingMapper.toEod(eodRepository.findTopByFechaContableOrderByFechaInicioDesc(date).orElseThrow(() -> new BusinessException("ACCOUNTING_EOD_NOT_FOUND", "No existe EOD para la fecha", HttpStatus.NOT_FOUND))); }

    @Transactional(readOnly = true)
    public TrialBalanceResponse obtenerBalance(LocalDate date) { return AccountingMapper.toTrialBalance(balanceRepository.findTopByFechaContableOrderByFechaGeneracionDesc(date).orElseThrow(() -> new BusinessException("ACCOUNTING_TRIAL_BALANCE_NOT_FOUND", "Balance de comprobación no encontrado", HttpStatus.NOT_FOUND))); }

    private BalanceComprobacion generarBalance(ProcesoEod eod) {
        List<CuentaContable> cuentas = cuentaRepository.findByEstadoOrderByCodigoContableAsc(EstadoCuentaContableEnum.ACTIVA);
        BigDecimal totalDeudor = BigDecimal.ZERO;
        BigDecimal totalAcreedor = BigDecimal.ZERO;
        for (CuentaContable c : cuentas) {
            if (c.getTipoCuenta() != TipoCuentaContableEnum.DETALLE) continue;
            BigDecimal saldo = c.getSaldoActual() == null ? BigDecimal.ZERO : c.getSaldoActual().abs();
            if (c.getNaturalezaSaldo() == NaturalezaSaldoEnum.DEUDORA) totalDeudor = totalDeudor.add(saldo); else totalAcreedor = totalAcreedor.add(saldo);
        }
        BalanceComprobacion balance = BalanceComprobacion.crear(eod, totalDeudor, totalAcreedor);
        int order = 1;
        for (CuentaContable c : cuentas) {
            if (c.getTipoCuenta() != TipoCuentaContableEnum.DETALLE) continue;
            BigDecimal saldo = c.getSaldoActual() == null ? BigDecimal.ZERO : c.getSaldoActual().abs();
            BigDecimal deudor = c.getNaturalezaSaldo() == NaturalezaSaldoEnum.DEUDORA ? saldo : BigDecimal.ZERO;
            BigDecimal acreedor = c.getNaturalezaSaldo() == NaturalezaSaldoEnum.ACREEDORA ? saldo : BigDecimal.ZERO;
            balance.agregarDetalle(BalanceComprobacionDetalle.crear(c, deudor, acreedor, order++));
        }
        return balanceRepository.save(balance);
    }

    private CuentaContable resolveCuenta(JournalEntryLineRequest line) {
        if (line.accountingCode() != null && !line.accountingCode().isBlank()) return getCuentaContable(line.accountingCode());
        if (line.institutionalAccountCode() != null && !line.institutionalAccountCode().isBlank()) return institucionalRepository.findByCodigoFuncional(line.institutionalAccountCode()).orElseThrow(() -> new BusinessException("ACCOUNTING_INSTITUTIONAL_ACCOUNT_NOT_FOUND", "Cuenta institucional no encontrada", HttpStatus.NOT_FOUND)).getCuentaContable();
        throw new BusinessException("ACCOUNTING_LINE_ACCOUNT_REQUIRED", "Cada línea debe enviar accountingCode o institutionalAccountCode", HttpStatus.BAD_REQUEST);
    }

    private CuentaContable getCuentaContable(String code) { return cuentaRepository.findByCodigoContable(code).orElseThrow(() -> new BusinessException("ACCOUNTING_ACCOUNT_NOT_FOUND", "Cuenta contable no encontrada", HttpStatus.NOT_FOUND)); }
    private AsientoContable getAsiento(String uuid) { return asientoRepository.findByUuidAsiento(uuid).orElseThrow(() -> new BusinessException("ACCOUNTING_JOURNAL_NOT_FOUND", "Asiento contable no encontrado", HttpStatus.NOT_FOUND)); }
    private JornadaContable getJornadaAbierta() { return jornadaRepository.findFirstByEstadoOrderByFechaContableDesc(EstadoJornadaContableEnum.ABIERTA).orElseThrow(() -> new BusinessException("ACCOUNTING_DATE_NOT_OPEN", "No existe una fecha contable abierta", HttpStatus.NOT_FOUND)); }
    private void validarFechaContableAbierta(LocalDate date) { JornadaContable jornada = jornadaRepository.findByFechaContable(date).orElseThrow(() -> new BusinessException("ACCOUNTING_DATE_NOT_FOUND", "Fecha contable no encontrada", HttpStatus.NOT_FOUND)); if (jornada.getEstado() != EstadoJornadaContableEnum.ABIERTA) throw new BusinessException("ACCOUNTING_DATE_NOT_OPEN", "La fecha contable no está abierta", HttpStatus.CONFLICT); }
    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String code) { try { return Enum.valueOf(type, value); } catch (Exception ex) { throw new BusinessException(code, "Valor inválido: " + value, HttpStatus.BAD_REQUEST); } }
}
