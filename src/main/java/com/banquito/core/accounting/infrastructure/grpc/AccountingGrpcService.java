package com.banquito.core.accounting.infrastructure.grpc;

import com.banquito.core.accounting.api.dto.api.JournalEntryRequest;
import com.banquito.core.accounting.application.service.AccountingService;
import com.banquito.core.accounting.shared.exception.BusinessException;
import tools.jackson.databind.ObjectMapper;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AccountingGrpcService extends AccountingServiceGrpc.AccountingServiceImplBase {
    private final AccountingService accountingService;
    private final ObjectMapper objectMapper;

    public AccountingGrpcService(AccountingService accountingService, ObjectMapper objectMapper) {
        this.accountingService = accountingService;
        this.objectMapper = objectMapper;
    }

    @Override public void getCurrentAccountingDate(CurrentAccountingDateRequest request, StreamObserver<AccountingDateReply> observer) {
        try { var r = accountingService.obtenerFechaContableActual(); observer.onNext(AccountingDateReply.newBuilder().setAccountingDate(r.accountingDate().toString()).setStatus(r.status()).build()); observer.onCompleted(); } catch (RuntimeException ex) { fail(observer, ex); }
    }
    @Override public void createJournalEntry(CreateJournalEntryRequest request, StreamObserver<JournalEntryReply> observer) {
        try { JournalEntryRequest dto = objectMapper.readValue(request.getPayloadJson(), JournalEntryRequest.class); var r = accountingService.crearAsiento(dto); observer.onNext(JournalEntryReply.newBuilder().setJournalEntryUuid(r.journalEntryUuid()).setStatus(r.status()).build()); observer.onCompleted(); } catch (Exception ex) { fail(observer, ex); }
    }
    @Override public void createReversalJournalEntry(CreateReversalJournalEntryRequest request, StreamObserver<JournalEntryReply> observer) {
        try { var r = accountingService.reversarAsiento(request.getOriginalJournalEntryUuid()); observer.onNext(JournalEntryReply.newBuilder().setJournalEntryUuid(r.journalEntryUuid()).setStatus(r.status()).build()); observer.onCompleted(); } catch (RuntimeException ex) { fail(observer, ex); }
    }
    @Override public void getInstitutionalAccountByFunctionalCode(InstitutionalAccountRequest request, StreamObserver<InstitutionalAccountReply> observer) {
        try { var r = accountingService.obtenerCuentaInstitucional(request.getFunctionalCode()); observer.onNext(InstitutionalAccountReply.newBuilder().setFunctionalCode(r.functionalCode()).setAccountingCode(r.accountingCode()).setStatus(r.status()).build()); observer.onCompleted(); } catch (RuntimeException ex) { fail(observer, ex); }
    }
    @Override public void runEod(RunEodRequest request, StreamObserver<EodReply> observer) {
        try { var r = accountingService.ejecutarEod(LocalDate.parse(request.getAccountingDate())); observer.onNext(EodReply.newBuilder().setEodUuid(r.eodUuid()).setStatus(r.status()).build()); observer.onCompleted(); } catch (RuntimeException ex) { fail(observer, ex); }
    }
    @Override public void getTrialBalance(TrialBalanceRequest request, StreamObserver<TrialBalanceReply> observer) {
        try { var r = accountingService.obtenerBalance(LocalDate.parse(request.getAccountingDate())); observer.onNext(TrialBalanceReply.newBuilder().setBalanceUuid(r.balanceUuid()).setStatus(r.status()).build()); observer.onCompleted(); } catch (RuntimeException ex) { fail(observer, ex); }
    }
    private static <T> void fail(StreamObserver<T> observer, Exception ex) {
        if (ex instanceof BusinessException be) observer.onError(Status.FAILED_PRECONDITION.withDescription(be.getCode() + "|" + be.getMessage()).asRuntimeException());
        else observer.onError(Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
}
