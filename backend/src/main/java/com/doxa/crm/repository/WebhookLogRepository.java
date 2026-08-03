package com.doxa.crm.repository;

import com.doxa.crm.domain.entity.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookLogRepository extends JpaRepository<WebhookLog, UUID> {
}
