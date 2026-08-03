package com.doxa.crm.repository;

import com.doxa.crm.domain.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StageRepository extends JpaRepository<Stage, UUID> {

    List<Stage> findByPipelineIdOrderByPositionAsc(UUID pipelineId);

    Optional<Stage> findByPipelineIdAndSlug(UUID pipelineId, String slug);
}
