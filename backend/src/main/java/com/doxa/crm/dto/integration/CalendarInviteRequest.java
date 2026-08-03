package com.doxa.crm.dto.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CalendarInviteRequest(
        @NotNull Instant startAt,
        Integer durationMinutes,
        String title
) {
    public int resolvedDurationMinutes() {
        return durationMinutes != null && durationMinutes > 0 ? durationMinutes : 30;
    }
}
