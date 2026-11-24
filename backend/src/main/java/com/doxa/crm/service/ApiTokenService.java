package com.doxa.crm.service;

import com.doxa.crm.domain.entity.ApiAccessToken;
import com.doxa.crm.domain.entity.User;
import com.doxa.crm.dto.developer.ApiTokenInspectionResponse;
import com.doxa.crm.dto.developer.ApiTokenResponse;
import com.doxa.crm.dto.developer.CreateApiTokenRequest;
import com.doxa.crm.dto.developer.CreatedApiTokenResponse;
import com.doxa.crm.exception.ResourceNotFoundException;
import com.doxa.crm.repository.ApiAccessTokenRepository;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.security.RolePolicy;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiTokenService {

    public static final String TOKEN_PREFIX = "doxa_live_";
    public static final Set<String> AVAILABLE_SCOPES = Set.of(
            "contacts:read",
            "contacts:write",
            "opportunities:read",
            "opportunities:write",
            "dashboard:read",
            "integrations:read",
            "integrations:write",
            "mcp:access"
    );

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiAccessTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Transactional
    public CreatedApiTokenResponse create(AuthUser admin, CreateApiTokenRequest request) {
        RolePolicy.requireAdmin(admin);
        if (request.expiresAt() != null && !request.expiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiration date must be in the future");
        }
        if (!AVAILABLE_SCOPES.containsAll(request.scopes())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more API token scopes are not supported");
        }

        String secret = generateSecret();
        String prefix = secret.substring(0, 20);
        ApiAccessToken token = ApiAccessToken.builder()
                .license(entityManager.getReference(com.doxa.crm.domain.entity.License.class, admin.getLicenseId()))
                .createdBy(entityManager.getReference(User.class, admin.getId()))
                .name(request.name().trim())
                .tokenPrefix(prefix)
                .tokenHash(passwordEncoder.encode(secret))
                .scopes(Set.copyOf(request.scopes()))
                .expiresAt(request.expiresAt())
                .build();

        return new CreatedApiTokenResponse(toResponse(tokenRepository.save(token)), secret);
    }

    @Transactional(readOnly = true)
    public List<ApiTokenResponse> list(AuthUser admin) {
        RolePolicy.requireAdmin(admin);
        return tokenRepository.findByLicenseIdOrderByCreatedAtDesc(admin.getLicenseId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ApiTokenResponse revoke(AuthUser admin, UUID id) {
        RolePolicy.requireAdmin(admin);
        ApiAccessToken token = tokenRepository.findByIdAndLicenseId(id, admin.getLicenseId())
                .orElseThrow(() -> new ResourceNotFoundException("API token not found"));
        if (token.getRevokedAt() == null) {
            token.setRevokedAt(Instant.now());
        }
        return toResponse(tokenRepository.save(token));
    }

    @Transactional(readOnly = true)
    public ApiTokenInspectionResponse inspect(AuthUser admin, String secret) {
        RolePolicy.requireAdmin(admin);
        ApiAccessToken token;
        try {
            token = findMatchingToken(secret, admin.getLicenseId());
        } catch (InvalidApiTokenException ex) {
            return new ApiTokenInspectionResponse("INVALID", null, null, Set.of(), null, null, null);
        }
        return new ApiTokenInspectionResponse(
                status(token),
                token.getCreatedBy().getEmail(),
                token.getName(),
                token.getScopes(),
                token.getCreatedAt(),
                token.getExpiresAt(),
                token.getLastUsedAt()
        );
    }

    @Transactional
    public AuthUser authenticate(String secret) {
        ApiAccessToken token = findMatchingToken(secret, null);
        if (!"ACTIVE".equals(status(token))) {
            throw new InvalidApiTokenException("API token is revoked or expired");
        }

        token.setLastUsedAt(Instant.now());
        tokenRepository.save(token);
        User owner = token.getCreatedBy();
        return new AuthUser(
                owner.getId(),
                token.getLicense().getId(),
                owner.getEmail(),
                owner.getPasswordHash(),
                owner.getRole(),
                token.getScopes(),
                true
        );
    }

    private ApiAccessToken findMatchingToken(String secret, UUID licenseId) {
        if (secret == null || !secret.startsWith(TOKEN_PREFIX) || secret.length() < TOKEN_PREFIX.length() + 32) {
            throw new InvalidApiTokenException("Invalid API token");
        }
        String prefix = secret.substring(0, 20);
        ApiAccessToken token = tokenRepository.findByTokenPrefix(prefix)
                .orElseThrow(() -> new InvalidApiTokenException("Invalid API token"));
        if (licenseId != null && !token.getLicense().getId().equals(licenseId)) {
            throw new ResourceNotFoundException("API token not found");
        }
        if (!passwordEncoder.matches(secret, token.getTokenHash())) {
            throw new InvalidApiTokenException("Invalid API token");
        }
        return token;
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ApiTokenResponse toResponse(ApiAccessToken token) {
        return new ApiTokenResponse(
                token.getId(),
                token.getName(),
                token.getTokenPrefix() + "...",
                token.getScopes(),
                token.getCreatedAt(),
                token.getExpiresAt(),
                token.getLastUsedAt(),
                token.getRevokedAt(),
                status(token)
        );
    }

    private String status(ApiAccessToken token) {
        if (token.getRevokedAt() != null) {
            return "REVOKED";
        }
        if (token.getExpiresAt() != null && !token.getExpiresAt().isAfter(Instant.now())) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }

    public static final class InvalidApiTokenException extends RuntimeException {
        public InvalidApiTokenException(String message) {
            super(message);
        }
    }
}
