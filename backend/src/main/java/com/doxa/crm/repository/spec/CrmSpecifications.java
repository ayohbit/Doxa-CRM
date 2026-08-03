package com.doxa.crm.repository.spec;

import com.doxa.crm.domain.entity.Contact;
import com.doxa.crm.domain.entity.Opportunity;
import com.doxa.crm.domain.enums.OpportunityStatus;
import com.doxa.crm.domain.enums.UserRole;
import com.doxa.crm.security.AuthUser;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public final class CrmSpecifications {

    private static final List<String> SDR_STAGE_SLUGS = List.of(
            "form-no-booking",
            "form-no-answer",
            "new-lead",
            "early-triage",
            "waiting-reply",
            "triage-not-qualified",
            "triage-no-show"
    );

    private CrmSpecifications() {
    }

    public static Specification<Opportunity> opportunityBelongsToLicense(UUID licenseId) {
        return (root, query, cb) -> cb.equal(root.get("license").get("id"), licenseId);
    }

    public static Specification<Opportunity> opportunityMatchesRole(AuthUser user) {
        return switch (user.getRole()) {
            case ADMIN -> (root, query, cb) -> cb.conjunction();
            case CLOSER -> (root, query, cb) -> cb.equal(root.get("assignedUser").get("id"), user.getId());
            case SDR -> (root, query, cb) -> root.get("stage").get("slug").in(SDR_STAGE_SLUGS);
        };
    }

    public static Specification<Opportunity> opportunityInStageSlug(String stageSlug) {
        if (stageSlug == null || stageSlug.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("stage").get("slug"), stageSlug);
    }

    public static Specification<Opportunity> opportunitySearch(String q) {
        if (q == null || q.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("contact").get("name")), pattern),
                cb.like(cb.lower(root.get("contact").get("email")), pattern),
                cb.like(cb.lower(root.get("adSet")), pattern)
        );
    }

    public static Specification<Opportunity> opportunityStatus(OpportunityStatus status) {
        if (status == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Contact> contactBelongsToLicense(UUID licenseId) {
        return (root, query, cb) -> cb.equal(root.get("license").get("id"), licenseId);
    }

    public static Specification<Contact> contactSearch(String q) {
        if (q == null || q.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("phone")), pattern)
        );
    }
}
