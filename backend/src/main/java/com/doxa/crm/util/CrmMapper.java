package com.doxa.crm.util;

import com.doxa.crm.domain.entity.Contact;
import com.doxa.crm.domain.entity.Opportunity;
import com.doxa.crm.domain.entity.OpportunityCall;
import com.doxa.crm.dto.contact.ContactResponse;
import com.doxa.crm.dto.opportunity.OpportunityResponse;

import java.math.BigDecimal;

public final class CrmMapper {

    private CrmMapper() {
    }

    public static OpportunityResponse toOpportunityResponse(Opportunity opportunity) {
        return toOpportunityResponse(opportunity, null);
    }

    public static OpportunityResponse toOpportunityResponse(Opportunity opportunity, OpportunityCall call) {
        Contact contact = opportunity.getContact();
        String phoneE164 = contact.getPhoneE164() != null
                ? contact.getPhoneE164()
                : PhoneNormalizer.toE164(contact.getPhone());
        String whatsAppUrl = WhatsAppLinkBuilder.buildUrl(phoneE164, null);
        boolean hasWrapUp = call != null && call.getOutcome() != null && !call.getOutcome().isBlank();
        BigDecimal callScore = call != null ? call.getAiScore() : null;

        return new OpportunityResponse(
                opportunity.getId(),
                contact.getId(),
                contact.getName(),
                opportunity.getStage().getSlug(),
                opportunity.getAdSet(),
                opportunity.getRevenueMonthly(),
                DateFormatter.formatOpportunityDate(opportunity.getCreatedAt()),
                opportunity.getValue(),
                contact.getEmail(),
                contact.getPhone(),
                phoneE164,
                whatsAppUrl,
                hasWrapUp,
                callScore
        );
    }

    public static ContactResponse toContactResponse(Contact contact) {
        return new ContactResponse(
                contact.getId(),
                contact.getName(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getTags(),
                DateFormatter.formatContactDate(contact.getCreatedAt())
        );
    }
}
