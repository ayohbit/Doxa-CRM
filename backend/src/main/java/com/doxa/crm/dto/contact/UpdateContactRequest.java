package com.doxa.crm.dto.contact;

import java.util.List;

public record UpdateContactRequest(
        String name,
        String email,
        String phone,
        List<String> tags
) {
}
