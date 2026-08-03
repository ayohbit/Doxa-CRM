package com.doxa.crm.controller;

import com.doxa.crm.dto.common.PageResponse;
import com.doxa.crm.dto.contact.ContactResponse;
import com.doxa.crm.dto.contact.CreateContactRequest;
import com.doxa.crm.dto.contact.UpdateContactRequest;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public PageResponse<ContactResponse> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return contactService.list(user, q, page, size);
    }

    @GetMapping("/{id}")
    public ContactResponse getById(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id
    ) {
        return contactService.getById(user, id);
    }

    @PostMapping
    public ResponseEntity<ContactResponse> create(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody CreateContactRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contactService.create(user, request));
    }

    @PutMapping("/{id}")
    public ContactResponse update(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id,
            @RequestBody UpdateContactRequest request
    ) {
        return contactService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id
    ) {
        contactService.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
