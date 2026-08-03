package com.doxa.crm;

import com.doxa.crm.config.TestDataConfig;
import com.doxa.crm.repository.ContactRepository;
import com.doxa.crm.repository.OpportunityRepository;
import com.doxa.crm.util.WebhookSignatureVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDataConfig.class)
class LeadBrokerWebhookTest {

    private static final String WEBHOOK_SECRET = "whsec_demo_license_secret_change_me";
    private static final String LICENSE_ID = "lic_demo";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Test
    void sameBrokerLeadIdIsIdempotent() throws Exception {
        long before = opportunityRepository.count();
        String body = samplePayload("brk_idempotent_001", "broker.lead@example.com", "+14075559901");

        MvcResult first = postWebhook(body);
        MvcResult second = postWebhook(body);

        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        assertThat(second.getResponse().getStatus()).isEqualTo(200);

        JsonNode firstJson = jsonMapper.readTree(first.getResponse().getContentAsString());
        JsonNode secondJson = jsonMapper.readTree(second.getResponse().getContentAsString());

        assertThat(firstJson.get("created").asBoolean()).isTrue();
        assertThat(secondJson.get("created").asBoolean()).isFalse();
        assertThat(secondJson.get("opportunityId").asText())
                .isEqualTo(firstJson.get("opportunityId").asText());
        assertThat(opportunityRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void invalidSignatureIsRejected() throws Exception {
        String body = samplePayload("brk_bad_sig_001", "bad.sig@example.com", "+14075559902");

        mockMvc.perform(post("/api/webhooks/lead-broker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Broker-Signature", "sha256=deadbeef")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateEmailReusesContact() throws Exception {
        String email = "dedupe.lead@example.com";
        String body1 = samplePayload("brk_dedupe_001", email, "+14075559903");
        String body2 = samplePayload("brk_dedupe_002", email, "+14075559903");

        MvcResult first = postWebhook(body1);
        MvcResult second = postWebhook(body2);

        JsonNode firstJson = jsonMapper.readTree(first.getResponse().getContentAsString());
        JsonNode secondJson = jsonMapper.readTree(second.getResponse().getContentAsString());

        assertThat(firstJson.get("contactId").asText()).isEqualTo(secondJson.get("contactId").asText());
        assertThat(firstJson.get("opportunityId").asText()).isNotEqualTo(secondJson.get("opportunityId").asText());
        assertThat(contactRepository.findAll().stream()
                .filter(c -> email.equalsIgnoreCase(c.getEmail()))
                .count()).isEqualTo(1);
    }

    private MvcResult postWebhook(String body) throws Exception {
        String signature = WebhookSignatureVerifier.sign(WEBHOOK_SECRET, body);
        return mockMvc.perform(post("/api/webhooks/lead-broker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Broker-Signature", signature)
                        .content(body))
                .andReturn();
    }

    private String samplePayload(String brokerLeadId, String email, String phone) {
        return """
                {
                  "event": "lead.purchased",
                  "broker_lead_id": "%s",
                  "license_id": "%s",
                  "purchased_at": "2026-07-28T14:32:00Z",
                  "price_paid": 45.00,
                  "currency": "USD",
                  "source": {
                    "campaign": "P2 | Broad | US & CAN | RE #1",
                    "platform": "meta"
                  },
                  "lead": {
                    "first_name": "Kumar",
                    "last_name": "Reyes",
                    "email": "%s",
                    "phone": "%s",
                    "revenue_monthly": "$10k - $25k/mo",
                    "consent": { "tcpa_opt_in": true, "collected_at": "2026-07-28T14:30:00Z" },
                    "custom_fields": {}
                  }
                }
                """.formatted(brokerLeadId, LICENSE_ID, email, phone);
    }
}
