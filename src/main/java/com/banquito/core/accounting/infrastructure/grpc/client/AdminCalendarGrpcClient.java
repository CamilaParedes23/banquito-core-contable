package com.banquito.core.accounting.infrastructure.grpc.client;

import com.banquito.core.accounting.shared.exception.BusinessException;
import com.banquito.core.admin.infrastructure.grpc.generated.AdminCatalogServiceGrpc;
import com.banquito.core.admin.infrastructure.grpc.generated.BusinessDayRequest;
import com.banquito.core.admin.infrastructure.grpc.generated.OperationalWindowCodeRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

@Component
public class AdminCalendarGrpcClient {

    private static final int MAX_DAYS_TO_SEARCH = 370;

    private final String host;
    private final int port;
    private final long timeoutMs;

    private ManagedChannel channel;
    private AdminCatalogServiceGrpc.AdminCatalogServiceBlockingStub stub;

    public AdminCalendarGrpcClient(
            @Value("${banquito.integration.admin-grpc-host:core-admin-service}") String host,
            @Value("${banquito.integration.admin-grpc-port:9093}") int port,
            @Value("${banquito.integration.grpc-timeout-ms:10000}") long timeoutMs) {
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
    }

    @PostConstruct
    public void init() {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = AdminCatalogServiceGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null) {
            channel.shutdown();
        }
    }

    public LocalDate obtenerSiguienteDiaHabil(LocalDate fechaActual) {
        LocalDate candidate = fechaActual;

        for (int i = 0; i < MAX_DAYS_TO_SEARCH; i++) {
            candidate = candidate.plusDays(1);
            BusinessDayInfo response = obtenerInformacionDia(candidate);

            if (response.businessDay()) {
                return candidate;
            }
        }

        throw new BusinessException(
                "ACCOUNTING_NEXT_BUSINESS_DAY_NOT_FOUND",
                "No fue posible determinar el siguiente día hábil",
                HttpStatus.CONFLICT);
    }

    public BusinessDayInfo obtenerInformacionDia(LocalDate date) {
        try {
            var response = stub.withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS)
                    .isBusinessDay(BusinessDayRequest.newBuilder()
                            .setDate(date.toString())
                            .build());

            return new BusinessDayInfo(
                    LocalDate.parse(response.getDate()),
                    response.getHoliday(),
                    response.getWeekend(),
                    response.getBusinessDay(),
                    blankToNull(response.getDescription()));
        } catch (StatusRuntimeException exception) {
            throw translateGrpcException(
                    exception,
                    "ACCOUNTING_ADMIN_CALENDAR_UNAVAILABLE",
                    "No fue posible consultar el calendario administrativo");
        }
    }

    public OperationalWindowInfo obtenerVentanaOperativa(String code) {
        try {
            var response = stub.withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS)
                    .getOperationalWindowByCode(
                            OperationalWindowCodeRequest.newBuilder()
                                    .setCode(code)
                                    .build());

            return new OperationalWindowInfo(
                    response.getCode(),
                    response.getName(),
                    response.getOperationalDomain(),
                    parseTime(response.getStartTime(), "hora de inicio"),
                    parseTime(response.getCutoffTime(), "hora de corte"),
                    parseTime(response.getEndTime(), "hora de fin"),
                    response.getActionAfterCutoff(),
                    response.getStatus());
        } catch (StatusRuntimeException exception) {
            throw translateGrpcException(
                    exception,
                    "ACCOUNTING_ADMIN_WINDOW_UNAVAILABLE",
                    "No fue posible consultar la ventana operativa contable");
        }
    }

    private LocalTime parseTime(String value, String field) {
        try {
            return LocalTime.parse(value);
        } catch (Exception exception) {
            throw new BusinessException(
                    "ACCOUNTING_OPERATIONAL_WINDOW_INVALID",
                    "La " + field + " de la ventana operativa no es válida",
                    HttpStatus.CONFLICT);
        }
    }

    private BusinessException translateGrpcException(
            StatusRuntimeException exception,
            String fallbackCode,
            String fallbackMessage) {
        String description = exception.getStatus().getDescription();

        if (description != null && description.contains("|")) {
            String[] parts = description.split("\\|", 2);
            return new BusinessException(parts[0], parts[1], HttpStatus.CONFLICT);
        }

        return new BusinessException(
                fallbackCode,
                fallbackMessage + ": "
                        + (description == null
                        ? exception.getStatus().getCode()
                        : description),
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record BusinessDayInfo(
            LocalDate date,
            boolean holiday,
            boolean weekend,
            boolean businessDay,
            String description
    ) {}

    public record OperationalWindowInfo(
            String code,
            String name,
            String operationalDomain,
            LocalTime startTime,
            LocalTime cutoffTime,
            LocalTime endTime,
            String actionAfterCutoff,
            String status
    ) {}
}
