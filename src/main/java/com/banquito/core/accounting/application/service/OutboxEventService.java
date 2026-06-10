package com.banquito.core.accounting.application.service;

import com.banquito.core.accounting.domain.model.OutboxEvent;
import com.banquito.core.accounting.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxEventService {
    private final OutboxEventRepository repository;
    public void registrar(String uuidCorrelacion, String tipoEvento, String agregadoTipo, String agregadoId, String payloadJson) {
        repository.save(OutboxEvent.crear(uuidCorrelacion, tipoEvento, agregadoTipo, agregadoId, payloadJson));
    }
}
