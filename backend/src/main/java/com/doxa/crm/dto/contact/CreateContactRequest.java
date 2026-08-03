package com.doxa.crm.dto.contact;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateContactRequest(
        @NotBlank String name,
        String email,
        String phone,
        List<String> tags
) {
}
