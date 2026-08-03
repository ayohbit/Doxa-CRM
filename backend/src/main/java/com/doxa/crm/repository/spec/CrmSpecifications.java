package com.doxa.crm.repository.spec;

import com.doxa.crm.domain.entity.Contact;
import com.doxa.crm.domain.entity.Opportunity;
import com.doxa.crm.domain.enums.OpportunityStatus;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.security.RolePolicy;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CrmSpecifications {

    private CrmSpecifications() {
    }

    public static Specification<Opportunity> opportunityBelongsToLicense(UUID licenseId) {
        return (root, query, cb) -> cb.equal(root.get("license").get("id"), licenseId);
    }

    public static Specification<Opportunity> opportunityMatchesRole(AuthUser user) {
        return switch (user.getRole()) {
            case ADMIN -> (root, query, cb) -> cb.conjunction();
            case CLOSER -> (root, query, cb) -> cb.equal(root.get("assignedUser").get("id"), user.getId());
            case SDR -> (root, query, cb) -> root.get("stage").get("slug").in(RolePolicy.SDR_STAGE_SLUGS);
        };
    }

    public static Specification<Opportunity> opportunityInStageSlug(String stageSlug) {
        if (stageSlug == null || stageSlug.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("stage").get("slug"), stageSlug);
    }

    public static Specification<Opportunity> opportunityStageSlugIn(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> root.get("stage").get("slug").in(slugs);
    }

    public static Specification<Opportunity> opportunityStageSlugNotIn(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.not(root.get("stage").get("slug").in(slugs));
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

    public static Specification<Opportunity> opportunityCreatedBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<Opportunity> opportunityUpdatedBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<Opportunity> opportunityAssignedTo(UUID assignedUserId) {
        if (assignedUserId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("assignedUser").get("id"), assignedUserId);
    }

    public static Specification<Opportunity> opportunityAdSet(String adSet) {
        if (adSet == null || adSet.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("adSet"), adSet);
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

    public static Specification<Contact> contactMatchesRole(AuthUser user) {
        return switch (user.getRole()) {
            case ADMIN -> (root, query, cb) -> cb.conjunction();
            case CLOSER -> contactLinkedToCloserOpportunities(user.getId());
            case SDR -> contactLinkedToSdrStageOpportunities();
        };
    }

    private static Specification<Contact> contactLinkedToCloserOpportunities(UUID closerId) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            var opportunity = subquery.from(Opportunity.class);
            subquery.select(cb.literal(1L));
            subquery.where(
                    cb.equal(opportunity.get("contact").get("id"), root.get("id")),
                    cb.equal(opportunity.get("assignedUser").get("id"), closerId)
            );
            return cb.exists(subquery);
        };
    }

    private static Specification<Contact> contactLinkedToSdrStageOpportunities() {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            var opportunity = subquery.from(Opportunity.class);
            subquery.select(cb.literal(1L));
            subquery.where(
                    cb.equal(opportunity.get("contact").get("id"), root.get("id")),
                    opportunity.get("stage").get("slug").in(RolePolicy.SDR_STAGE_SLUGS)
            );
            return cb.exists(subquery);
        };
    }

    public static Specification<Opportunity> fetchContactAndStage() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("contact", JoinType.INNER);
                root.fetch("stage", JoinType.INNER);
                query.distinct(true);
            }
            return cb.conjunction();
        };
    }
}
