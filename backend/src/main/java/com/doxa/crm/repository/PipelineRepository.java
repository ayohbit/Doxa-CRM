package com.doxa.crm.repository;

import com.doxa.crm.domain.entity.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PipelineRepository extends JpaRepository<Pipeline, UUID> {

    Optional<Pipeline> findByLicenseIdAndName(UUID licenseId, String name);
}
