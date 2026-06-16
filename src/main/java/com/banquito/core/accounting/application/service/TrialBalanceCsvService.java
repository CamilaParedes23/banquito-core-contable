package com.banquito.core.accounting.application.service;

import com.banquito.core.accounting.domain.model.BalanceComprobacion;
import com.banquito.core.accounting.domain.model.BalanceComprobacionDetalle;
import com.banquito.core.accounting.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;

@Slf4j
@Service
public class TrialBalanceCsvService {
    private static final String HEADER = "Código de Cuenta,Nombre de Cuenta,Saldo Deudor,Saldo Acreedor\r\n";

    private final Path reportDirectory;

    public TrialBalanceCsvService(
            @Value("${banquito.accounting.report-directory:./reports/eod}") String reportDirectory) {
        this.reportDirectory = Path.of(reportDirectory).toAbsolutePath().normalize();
    }

    public String write(BalanceComprobacion balance) {
        String documentUuid = UUID.randomUUID().toString();
        Path target = resolve(documentUuid);
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(reportDirectory);
            Files.writeString(temporary, buildCsv(balance), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveNotSupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return documentUuid;
        } catch (IOException ex) {
            deleteQuietly(temporary);
            log.error("No fue posible escribir el balance CSV en el directorio {}. Tipo={}, mensaje={}",
                    reportDirectory, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            throw new BusinessException(
                    "ACCOUNTING_TRIAL_BALANCE_EXPORT_FAILED",
                    "No fue posible generar el archivo CSV del balance de comprobación",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public byte[] read(String documentUuid) {
        validateUuid(documentUuid);
        Path report = resolve(documentUuid);
        if (!Files.isRegularFile(report)) {
            throw new BusinessException(
                    "ACCOUNTING_TRIAL_BALANCE_EXPORT_NOT_FOUND",
                    "El archivo CSV del balance de comprobación no está disponible",
                    HttpStatus.NOT_FOUND);
        }
        try {
            return Files.readAllBytes(report);
        } catch (IOException ex) {
            throw new BusinessException(
                    "ACCOUNTING_TRIAL_BALANCE_EXPORT_READ_FAILED",
                    "No fue posible leer el archivo CSV del balance de comprobación",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Path resolve(String documentUuid) {
        Path resolved = reportDirectory.resolve(documentUuid + ".csv").normalize();
        if (!resolved.startsWith(reportDirectory)) {
            throw new BusinessException(
                    "ACCOUNTING_TRIAL_BALANCE_EXPORT_INVALID",
                    "Identificador de reporte inválido",
                    HttpStatus.BAD_REQUEST);
        }
        return resolved;
    }

    private String buildCsv(BalanceComprobacion balance) {
        StringBuilder csv = new StringBuilder("\uFEFF").append(HEADER);
        balance.getDetalles().stream()
                .sorted(Comparator.comparing(BalanceComprobacionDetalle::getOrdenLinea))
                .forEach(detail -> csv.append(escape(detail.getCodigoContable())).append(',')
                        .append(escape(detail.getNombreCuenta())).append(',')
                        .append(amount(detail.getSaldoDeudor())).append(',')
                        .append(amount(detail.getSaldoAcreedor())).append("\r\n"));
        csv.append("TOTAL,")
                .append(escape("Totales del balance de comprobación")).append(',')
                .append(amount(balance.getTotalSaldoDeudor())).append(',')
                .append(amount(balance.getTotalSaldoAcreedor())).append("\r\n");
        return csv.toString();
    }

    private String escape(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return '"' + safe + '"';
    }

    private String amount(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void validateUuid(String value) {
        try {
            UUID.fromString(value);
        } catch (Exception ex) {
            throw new BusinessException(
                    "ACCOUNTING_TRIAL_BALANCE_EXPORT_INVALID",
                    "Identificador de reporte inválido",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // La limpieza del temporal no debe ocultar el error funcional original.
        }
    }
}
