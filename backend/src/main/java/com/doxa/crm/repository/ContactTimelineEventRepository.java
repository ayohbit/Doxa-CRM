package com.doxa.crm.repository;

import com.doxa.crm.domain.entity.ContactTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContactTimelineEventRepository extends JpaRepository<ContactTimelineEvent, UUID> {

    List<ContactTimelineEvent> findByContactIdOrderByCreatedAtDesc(UUID contactId);
}
