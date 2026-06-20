package com.banquito.core.accounting.application.service;

import com.banquito.core.accounting.api.dto.api.*;
import com.banquito.core.accounting.domain.enums.TipoMovimientoContableEnum;
import com.banquito.core.accounting.domain.model.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;

public final class AccountingMapper {
    private AccountingMapper() {}

    public static ChartAccountResponse toChartAccount(CuentaContable c) {
        return new ChartAccountResponse(c.getCodigoContable(), c.getNombre(), c.getClase().name(), c.getNaturalezaSaldo().name(), c.getTipoCuenta().name(), c.getCuentaPadre() != null ? c.getCuentaPadre().getCodigoContable() : null, c.getNivel(), c.getPermiteMovimientos(), c.getSaldoActual(), c.getEstado().name());
    }

    public static InstitutionalAccountResponse toInstitutional(CuentaInstitucional c) {
        return new InstitutionalAccountResponse(c.getCodigoFuncional(), c.getNombre(), c.getTipoCuenta().name(), c.getEstado().name(), c.getCuentaContable().getCodigoContable(), c.getCuentaContable().getNombre());
    }

    public static AccountingDateResponse toAccountingDate(JornadaContable j) {
        return new AccountingDateResponse(j.getId(), j.getFechaContable(), j.getEstado().name(), j.getHoraCorte(), j.getFechaApertura(), j.getFechaCierre(), j.getObservacion());
    }


    public static JournalEntrySummaryResponse toJournalEntrySummary(
            AsientoContable entry,
            Collection<DetalleAsientoContable> lines) {
        BigDecimal totalDebit = lines.stream()
                .filter(line -> line.getTipoMovimiento()
                        == TipoMovimientoContableEnum.DEBITO)
                .map(DetalleAsientoContable::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream()
                .filter(line -> line.getTipoMovimiento()
                        == TipoMovimientoContableEnum.CREDITO)
                .map(DetalleAsientoContable::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new JournalEntrySummaryResponse(
                entry.getUuidAsiento(),
                entry.getUuidCorrelacion(),
                entry.getTransaccionUuid(),
                entry.getContextoOrigen().name(),
                entry.getTipoOperacion(),
                entry.getDescripcion(),
                entry.getFechaContable(),
                entry.getTimestampRegistro(),
                entry.getEstado().name(),
                totalDebit,
                totalCredit,
                totalDebit.compareTo(totalCredit) == 0);
    }

    public static JournalEntryResponse toJournalEntry(AsientoContable a) {
        return new JournalEntryResponse(a.getUuidAsiento(), a.getUuidCorrelacion(), a.getTransaccionUuid(), a.getContextoOrigen().name(), a.getTipoOperacion(), a.getDescripcion(), a.getFechaContable(), a.getTimestampRegistro(), a.getEstado().name(), a.getReferenciaExterna(), a.getDetalles().stream().sorted(Comparator.comparing(DetalleAsientoContable::getOrdenLinea)).map(d -> new JournalEntryLineResponse(d.getOrdenLinea(), d.getCuentaContable().getCodigoContable(), d.getCuentaContable().getNombre(), d.getTipoMovimiento().name(), d.getMonto(), d.getReferencia())).toList());
    }

    public static EodResponse toEod(ProcesoEod p) {
        return toEod(p, null);
    }

    public static EodResponse toEod(ProcesoEod p, JornadaContable nextAccountingDate) {
        return new EodResponse(
                p.getUuidEod(),
                p.getFechaContable(),
                p.getEstado().name(),
                p.getTotalDebitos(),
                p.getTotalCreditos(),
                p.getUuidDocumentoReporte(),
                p.getMensajeError(),
                p.getFechaInicio(),
                p.getFechaFin(),
                nextAccountingDate != null ? nextAccountingDate.getFechaContable() : null,
                nextAccountingDate != null ? nextAccountingDate.getEstado().name() : null);
    }

    public static TrialBalanceResponse toTrialBalance(BalanceComprobacion b) {
        return new TrialBalanceResponse(b.getUuidBalance(), b.getFechaContable(), b.getTotalSaldoDeudor(), b.getTotalSaldoAcreedor(), b.getEstado().name(), b.getFechaGeneracion(), b.getUuidDocumentoCsv(), b.getDetalles().stream().sorted(Comparator.comparing(BalanceComprobacionDetalle::getOrdenLinea)).map(d -> new TrialBalanceDetailResponse(d.getOrdenLinea(), d.getCodigoContable(), d.getNombreCuenta(), d.getSaldoDeudor(), d.getSaldoAcreedor())).toList());
    }
}
