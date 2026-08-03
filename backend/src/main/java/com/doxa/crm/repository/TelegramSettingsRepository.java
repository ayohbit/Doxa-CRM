package com.doxa.crm.repository;

import com.doxa.crm.domain.entity.TelegramSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TelegramSettingsRepository extends JpaRepository<TelegramSettings, UUID> {

    Optional<TelegramSettings> findByLicenseIdAndEnabledTrue(UUID licenseId);
}
