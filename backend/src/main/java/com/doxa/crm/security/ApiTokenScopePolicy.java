package com.doxa.crm.security;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.Map;

public final class ApiTokenScopePolicy {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final Map<String, String> FIXED_SCOPES = Map.of(
            "GET /api/pipelines/board", "opportunities:read",
            "GET /api/dashboard/**", "dashboard:read",
            "GET /api/integrations/status", "integrations:read",
            "GET /api/integrations/google/auth-url", "integrations:write",
            "POST /mcp", "mcp:access",
            "GET /mcp", "mcp:access"
    );

    private ApiTokenScopePolicy() {
    }

    public static String requiredScope(String method, String path) {
        String scope = FIXED_SCOPES.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(method + " "))
                .filter(entry -> PATH_MATCHER.match(entry.getKey().substring(method.length() + 1), path))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (scope != null) {
            return scope;
        }
        if (PATH_MATCHER.match("/api/contacts/*/timeline", path)) {
            return "contacts:read";
        }
        if (PATH_MATCHER.match("/api/contacts/*/email/send", path)) {
            return "integrations:write";
        }
        if (PATH_MATCHER.match("/api/contacts", path) || PATH_MATCHER.match("/api/contacts/**", path)) {
            return isRead(method) ? "contacts:read" : "contacts:write";
        }
        if (PATH_MATCHER.match("/api/opportunities", path) || PATH_MATCHER.match("/api/opportunities/**", path)) {
            return isRead(method) ? "opportunities:read" : "opportunities:write";
        }
        return null;
    }

    private static boolean isRead(String method) {
        return HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method);
    }
}
