package com.doxa.crm.service;

import com.doxa.crm.domain.entity.Pipeline;
import com.doxa.crm.domain.entity.Stage;
import com.doxa.crm.domain.enums.OpportunityStatus;
import com.doxa.crm.dto.pipeline.PipelineBoardResponse;
import com.doxa.crm.dto.pipeline.StageResponse;
import com.doxa.crm.exception.ResourceNotFoundException;
import com.doxa.crm.repository.OpportunityRepository;
import com.doxa.crm.repository.PipelineRepository;
import com.doxa.crm.repository.StageRepository;
import com.doxa.crm.repository.spec.CrmSpecifications;
import com.doxa.crm.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PipelineService {

    public static final String ADS_PIPELINE_NAME = "Ads Pipeline";

    private final PipelineRepository pipelineRepository;
    private final StageRepository stageRepository;
    private final OpportunityRepository opportunityRepository;

    @Transactional(readOnly = true)
    public PipelineBoardResponse getBoard(AuthUser user) {
        Pipeline pipeline = pipelineRepository.findByLicenseIdAndName(user.getLicenseId(), ADS_PIPELINE_NAME)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found"));

        List<Stage> stages = stageRepository.findByPipelineIdOrderByPositionAsc(pipeline.getId());

        List<StageResponse> stageResponses = stages.stream()
                .map(stage -> toStageResponse(stage, user))
                .toList();

        long total = opportunityRepository.count(
                Specification.where(CrmSpecifications.opportunityBelongsToLicense(user.getLicenseId()))
                        .and(CrmSpecifications.opportunityMatchesRole(user))
                        .and(CrmSpecifications.opportunityStatus(OpportunityStatus.OPEN))
        );

        return new PipelineBoardResponse(pipeline.getName(), total, stageResponses);
    }

    @Transactional(readOnly = true)
    public Stage resolveStage(AuthUser user, String stageSlug) {
        Pipeline pipeline = pipelineRepository.findByLicenseIdAndName(user.getLicenseId(), ADS_PIPELINE_NAME)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found"));

        return stageRepository.findByPipelineIdAndSlug(pipeline.getId(), stageSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Stage not found: " + stageSlug));
    }

    private StageResponse toStageResponse(Stage stage, AuthUser user) {
        UUID licenseId = user.getLicenseId();
        long count = opportunityRepository.count(
                Specification.where(CrmSpecifications.opportunityBelongsToLicense(licenseId))
                        .and(CrmSpecifications.opportunityMatchesRole(user))
                        .and(CrmSpecifications.opportunityInStageSlug(stage.getSlug()))
                        .and(CrmSpecifications.opportunityStatus(OpportunityStatus.OPEN))
        );

        BigDecimal totalValue = opportunityRepository.sumValueByLicenseStageAndStatus(
                licenseId, stage.getId(), OpportunityStatus.OPEN
        );

        return new StageResponse(
                stage.getSlug(),
                stage.getName(),
                stage.getMonetaryValue(),
                count,
                totalValue
        );
    }
}
