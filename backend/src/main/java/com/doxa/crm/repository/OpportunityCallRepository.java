package com.doxa.crm.repository;

import com.doxa.crm.domain.entity.OpportunityCall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpportunityCallRepository extends JpaRepository<OpportunityCall, UUID> {

    Optional<OpportunityCall> findByOpportunity_Id(UUID opportunityId);

    List<OpportunityCall> findByOpportunity_IdIn(Collection<UUID> opportunityIds);
}
