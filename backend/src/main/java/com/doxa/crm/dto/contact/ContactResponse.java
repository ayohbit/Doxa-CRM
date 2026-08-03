package com.doxa.crm.dto.contact;

import java.util.List;
import java.util.UUID;

public record ContactResponse(
        UUID id,
        String name,
        String email,
        String phone,
        List<String> tags,
        String created
) {
}
