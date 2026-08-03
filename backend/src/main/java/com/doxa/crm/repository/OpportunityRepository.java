package com.doxa.crm.repository;

import com.doxa.crm.domain.entity.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {
}
