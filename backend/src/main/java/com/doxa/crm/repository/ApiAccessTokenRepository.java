package com.doxa.crm.repository;

import com.doxa.crm.domain.entity.ApiAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiAccessTokenRepository extends JpaRepository<ApiAccessToken, UUID> {

    Optional<ApiAccessToken> findByTokenPrefix(String tokenPrefix);

    Optional<ApiAccessToken> findByIdAndLicenseId(UUID id, UUID licenseId);

    List<ApiAccessToken> findByLicenseIdOrderByCreatedAtDesc(UUID licenseId);
}
