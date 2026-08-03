package com.doxa.crm.repository;

import com.doxa.crm.domain.entity.Opportunity;
import com.doxa.crm.domain.enums.OpportunityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpportunityRepository extends JpaRepository<Opportunity, UUID>, JpaSpecificationExecutor<Opportunity> {

    @Query("""
            SELECT COUNT(o) FROM Opportunity o
            WHERE o.license.id = :licenseId
              AND o.stage.id = :stageId
              AND o.status = :status
            """)
    long countByLicenseStageAndStatus(
            @Param("licenseId") UUID licenseId,
            @Param("stageId") UUID stageId,
            @Param("status") OpportunityStatus status
    );

    @Query("""
            SELECT COALESCE(SUM(o.value), 0) FROM Opportunity o
            WHERE o.license.id = :licenseId
              AND o.stage.id = :stageId
              AND o.status = :status
            """)
    BigDecimal sumValueByLicenseStageAndStatus(
            @Param("licenseId") UUID licenseId,
            @Param("stageId") UUID stageId,
            @Param("status") OpportunityStatus status
    );

    @Query("""
            SELECT COUNT(o) FROM Opportunity o
            WHERE o.license.id = :licenseId
              AND o.status = :status
            """)
    long countByLicenseAndStatus(
            @Param("licenseId") UUID licenseId,
            @Param("status") OpportunityStatus status
    );

    Optional<Opportunity> findByIdAndLicenseId(UUID id, UUID licenseId);

    long countByContactId(UUID contactId);

    @Query("""
            SELECT DISTINCT o.adSet FROM Opportunity o
            WHERE o.license.id = :licenseId AND o.adSet IS NOT NULL
            ORDER BY o.adSet
            """)
    List<String> findDistinctAdSets(@Param("licenseId") UUID licenseId);

    Optional<Opportunity> findByLicenseIdAndBrokerLeadId(UUID licenseId, String brokerLeadId);
}
