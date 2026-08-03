package com.doxa.crm.dto.opportunity;

import jakarta.validation.constraints.NotBlank;

public record MoveStageRequest(
        @NotBlank String stageSlug
) {
}
