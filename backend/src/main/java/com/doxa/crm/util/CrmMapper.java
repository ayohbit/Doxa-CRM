package com.doxa.crm.util;

import com.doxa.crm.domain.entity.Contact;
import com.doxa.crm.domain.entity.Opportunity;
import com.doxa.crm.dto.contact.ContactResponse;
import com.doxa.crm.dto.opportunity.OpportunityResponse;

public final class CrmMapper {

    private CrmMapper() {
    }

    public static OpportunityResponse toOpportunityResponse(Opportunity opportunity) {
        Contact contact = opportunity.getContact();
        return new OpportunityResponse(
                opportunity.getId(),
                contact.getName(),
                opportunity.getStage().getSlug(),
                opportunity.getAdSet(),
                opportunity.getRevenueMonthly(),
                DateFormatter.formatOpportunityDate(opportunity.getCreatedAt()),
                opportunity.getValue(),
                contact.getEmail(),
                contact.getPhone()
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
