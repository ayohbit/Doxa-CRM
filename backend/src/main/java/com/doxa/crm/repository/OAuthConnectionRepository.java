package com.doxa.crm.repository;

import com.doxa.crm.domain.entity.OAuthConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthConnectionRepository extends JpaRepository<OAuthConnection, UUID> {

    Optional<OAuthConnection> findByUserId(UUID userId);
}
