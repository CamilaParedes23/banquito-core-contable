package com.banquito.core.accounting.application.service;

import com.banquito.core.accounting.domain.enums.ResultadoAuditoriaAccountingEnum;
import com.banquito.core.accounting.domain.model.AuditoriaAccountingEvento;
import com.banquito.core.accounting.domain.repository.AuditoriaAccountingEventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditoriaAccountingService {
    private final AuditoriaAccountingEventoRepository repository;
    public void registrar(String uuidCorrelacion, String accion, String entidad, String entidadId, ResultadoAuditoriaAccountingEnum resultado, String detalleJson) {
        repository.save(AuditoriaAccountingEvento.registrar(uuidCorrelacion, "ACCOUNTING", accion, entidad, entidadId, resultado, detalleJson));
    }
}
