package com.banquito.core.accounting.domain.repository;

import com.banquito.core.accounting.domain.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
}
