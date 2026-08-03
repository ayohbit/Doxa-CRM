package com.doxa.crm.repository;

import com.doxa.crm.domain.entity.License;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LicenseRepository extends JpaRepository<License, UUID> {
    long count();
}
