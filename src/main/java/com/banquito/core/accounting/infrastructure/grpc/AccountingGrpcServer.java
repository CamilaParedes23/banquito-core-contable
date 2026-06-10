package com.banquito.core.accounting.infrastructure.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AccountingGrpcServer {
    private static final Logger log = LoggerFactory.getLogger(AccountingGrpcServer.class);
    private final AccountingGrpcService accountingGrpcService;
    private final int port;
    private Server server;

    public AccountingGrpcServer(AccountingGrpcService accountingGrpcService,
                                @Value("${banquito.grpc.server.port:9094}") int port) {
        this.accountingGrpcService = accountingGrpcService;
        this.port = port;
    }
    @PostConstruct public void start() throws Exception { this.server = ServerBuilder.forPort(port).addService(accountingGrpcService).build().start(); log.info("Accounting gRPC server started on port {}", port); }
    @PreDestroy public void stop() { if (server != null) { server.shutdown(); log.info("Accounting gRPC server stopped"); } }
}
