package com.doxa.crm;

import com.doxa.crm.config.TestDataConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDataConfig.class)
class TenantIsolationTest {

    private static final String OTHER_TENANT_CONTACT_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String OTHER_TENANT_OPPORTUNITY_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void demoTenantCannotReadOtherTenantContact() throws Exception {
        String token = login("admin@demo.doxa.com");

        mockMvc.perform(get("/api/contacts/" + OTHER_TENANT_CONTACT_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void demoTenantCannotReadOtherTenantOpportunity() throws Exception {
        String token = login("admin@demo.doxa.com");

        mockMvc.perform(get("/api/opportunities/" + OTHER_TENANT_OPPORTUNITY_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void closerCannotReadUnassignedOpportunityFromOwnTenant() throws Exception {
        String adminToken = login("admin@demo.doxa.com");
        MvcResult listResult = mockMvc.perform(get("/api/opportunities?page=0&size=1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = jsonMapper.readTree(listResult.getResponse().getContentAsString());
        if (body.get("content").isEmpty()) {
            return;
        }

        String opportunityId = body.get("content").get(0).get("id").asText();
        String closerToken = login("closer@demo.doxa.com");

        int status = mockMvc.perform(get("/api/opportunities/" + opportunityId)
                        .header("Authorization", "Bearer " + closerToken))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(403, 404);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = jsonMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
