package com.doxa.crm.service;

import com.doxa.crm.domain.entity.Contact;
import com.doxa.crm.domain.entity.License;
import com.doxa.crm.dto.common.PageResponse;
import com.doxa.crm.dto.contact.ContactResponse;
import com.doxa.crm.dto.contact.CreateContactRequest;
import com.doxa.crm.dto.contact.UpdateContactRequest;
import com.doxa.crm.exception.AccessDeniedException;
import com.doxa.crm.exception.ResourceNotFoundException;
import com.doxa.crm.repository.ContactRepository;
import com.doxa.crm.repository.LicenseRepository;
import com.doxa.crm.repository.OpportunityRepository;
import com.doxa.crm.repository.spec.CrmSpecifications;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.security.RolePolicy;
import com.doxa.crm.util.CrmMapper;
import com.doxa.crm.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final LicenseRepository licenseRepository;
    private final OpportunityRepository opportunityRepository;

    @Transactional(readOnly = true)
    public PageResponse<ContactResponse> list(AuthUser user, String search, int page, int size) {
        Specification<Contact> spec = Specification
                .where(CrmSpecifications.contactBelongsToLicense(user.getLicenseId()))
                .and(CrmSpecifications.contactMatchesRole(user))
                .and(CrmSpecifications.contactSearch(search));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ContactResponse> result = contactRepository.findAll(spec, pageable)
                .map(CrmMapper::toContactResponse);

        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public ContactResponse getById(AuthUser user, UUID id) {
        Contact contact = findAccessible(id, user);
        return CrmMapper.toContactResponse(contact);
    }

    @Transactional
    public ContactResponse create(AuthUser user, CreateContactRequest request) {
        RolePolicy.requireWriteAccess(user);

        License license = licenseRepository.findById(user.getLicenseId())
                .orElseThrow(() -> new ResourceNotFoundException("License not found"));

        String phoneE164 = PhoneNormalizer.toE164(request.phone());
        Contact contact = Contact.builder()
                .license(license)
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .phoneE164(phoneE164)
                .tags(request.tags() != null ? new ArrayList<>(request.tags()) : new ArrayList<>())
                .dedupeKey(PhoneNormalizer.dedupeKey(request.email(), phoneE164))
                .build();

        return CrmMapper.toContactResponse(contactRepository.save(contact));
    }

    @Transactional
    public ContactResponse update(AuthUser user, UUID id, UpdateContactRequest request) {
        RolePolicy.requireWriteAccess(user);
        Contact contact = findAccessible(id, user);

        if (request.name() != null && !request.name().isBlank()) {
            contact.setName(request.name());
        }
        if (request.email() != null) {
            contact.setEmail(request.email());
        }
        if (request.phone() != null) {
            contact.setPhone(request.phone());
            contact.setPhoneE164(PhoneNormalizer.toE164(request.phone()));
        }
        if (request.tags() != null) {
            contact.setTags(new ArrayList<>(request.tags()));
        }

        contact.setDedupeKey(PhoneNormalizer.dedupeKey(contact.getEmail(), contact.getPhoneE164()));

        return CrmMapper.toContactResponse(contactRepository.save(contact));
    }

    @Transactional
    public void delete(AuthUser user, UUID id) {
        RolePolicy.requireAdmin(user);
        Contact contact = findAccessible(id, user);

        if (opportunityRepository.countByContactId(contact.getId()) > 0) {
            throw new IllegalStateException("Cannot delete contact with linked opportunities");
        }
        contactRepository.delete(contact);
    }

    private Contact findAccessible(UUID id, UUID licenseId) {
        return contactRepository.findByIdAndLicenseId(id, licenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
    }

    private Contact findAccessible(UUID id, AuthUser user) {
        Contact contact = findAccessible(id, user.getLicenseId());
        verifyContactAccess(contact, user);
        return contact;
    }

    private void verifyContactAccess(Contact contact, AuthUser user) {
        Specification<Contact> spec = Specification
                .where(CrmSpecifications.contactBelongsToLicense(user.getLicenseId()))
                .and(CrmSpecifications.contactMatchesRole(user))
                .and((root, query, cb) -> cb.equal(root.get("id"), contact.getId()));

        if (contactRepository.count(spec) == 0) {
            throw new AccessDeniedException("You do not have access to this contact");
        }
    }
}
