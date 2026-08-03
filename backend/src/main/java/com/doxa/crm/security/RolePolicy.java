package com.doxa.crm.security;

import com.doxa.crm.domain.enums.UserRole;
import com.doxa.crm.exception.AccessDeniedException;

import java.util.List;
import java.util.UUID;

public final class RolePolicy {

    public static final List<String> SDR_STAGE_SLUGS = List.of(
            "form-no-booking",
            "form-no-answer",
            "new-lead",
            "early-triage",
            "waiting-reply",
            "triage-not-qualified",
            "triage-no-show"
    );

    public static final List<String> SC_BOOKED_STAGE_SLUGS = List.of(
            "qualified",
            "reconnect-1",
            "reconnect-2"
    );

    public static final List<String> SC_SHOWN_STAGE_SLUGS = List.of(
            "reconnect-1",
            "reconnect-2"
    );

    private RolePolicy() {
    }

    public static void requireAdmin(AuthUser user) {
        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Admin role required");
        }
    }

    public static void requireWriteAccess(AuthUser user) {
        if (user.getRole() == UserRole.CLOSER) {
            throw new AccessDeniedException("Closers have read-only access to contacts");
        }
    }

    public static UUID effectiveAssignedUserFilter(AuthUser user, UUID requestedAssignedUserId) {
        return switch (user.getRole()) {
            case ADMIN -> requestedAssignedUserId;
            case CLOSER -> user.getId();
            case SDR -> requestedAssignedUserId;
        };
    }
}
