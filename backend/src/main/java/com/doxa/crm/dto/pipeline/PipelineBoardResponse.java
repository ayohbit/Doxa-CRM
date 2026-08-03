package com.doxa.crm.dto.pipeline;

import java.util.List;

public record PipelineBoardResponse(
        String pipelineName,
        long totalOpportunities,
        List<StageResponse> stages
) {
}
