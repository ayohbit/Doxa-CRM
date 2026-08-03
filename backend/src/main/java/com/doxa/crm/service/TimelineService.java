package com.doxa.crm.service;

import com.doxa.crm.domain.entity.Contact;
import com.doxa.crm.domain.entity.ContactTimelineEvent;
import com.doxa.crm.domain.entity.License;
import com.doxa.crm.domain.entity.User;
import com.doxa.crm.domain.enums.TimelineEventType;
import com.doxa.crm.dto.integration.TimelineEventResponse;
import com.doxa.crm.repository.ContactTimelineEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private final ContactTimelineEventRepository timelineRepository;

    @Transactional
    public ContactTimelineEvent append(
            Contact contact,
            License license,
            TimelineEventType type,
            String title,
            String body,
            Map<String, Object> metadata,
            User createdBy
    ) {
        ContactTimelineEvent event = ContactTimelineEvent.builder()
                .contact(contact)
                .license(license)
                .eventType(type)
                .title(title)
                .body(body)
                .metadata(metadata != null ? metadata : Map.of())
                .createdBy(createdBy)
                .build();
        return timelineRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> listForContact(UUID contactId) {
        return timelineRepository.findByContactIdOrderByCreatedAtDesc(contactId).stream()
                .map(event -> new TimelineEventResponse(
                        event.getId(),
                        event.getEventType().name(),
                        event.getTitle(),
                        event.getBody(),
                        event.getCreatedAt(),
                        event.getCreatedBy() != null ? event.getCreatedBy().getEmail() : null
                ))
                .toList();
    }
}
