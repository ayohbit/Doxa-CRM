package com.doxa.crm.repository;

import com.doxa.crm.domain.entity.StageHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StageHistoryRepository extends JpaRepository<StageHistory, UUID> {
}
