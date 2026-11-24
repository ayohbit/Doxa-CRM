package com.doxa.crm;

import com.doxa.crm.config.TestDataConfig;
import com.doxa.crm.repository.ApiAccessTokenRepository;
import com.doxa.crm.security.ApiTokenScopePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDataConfig.class)
class DeveloperPortalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private ApiAccessTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void apiTokenIsHashedScopedAndRevocable() throws Exception {
        String adminJwt = login("admin@demo.doxa.com");
        JsonNode created = createToken(adminJwt, "[\"contacts:read\"]");
        String secret = created.path("secret").asText();
        String tokenId = created.path("token").path("id").asText();
        String prefix = secret.substring(0, 20);

        assertThat(secret).startsWith("doxa_live_");
        var stored = tokenRepository.findByTokenPrefix(prefix).orElseThrow();
        assertThat(stored.getTokenHash()).isNotEqualTo(secret);
        assertThat(passwordEncoder.matches(secret, stored.getTokenHash())).isTrue();
        assertThat(stored.getScopes()).containsExactly("contacts:read");
        assertThat(ApiTokenScopePolicy.requiredScope("GET", "/api/contacts")).isEqualTo("contacts:read");

        mockMvc.perform(get("/api/contacts")
                        .header("Authorization", "Bearer " + secret))
                .andExpect(status().isOk());
        assertThat(tokenRepository.findByTokenPrefix(prefix).orElseThrow().getLastUsedAt()).isNotNull();
        mockMvc.perform(get("/api/opportunities")
                        .header("Authorization", "Bearer " + secret))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/developer/tokens/" + tokenId)
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/contacts")
                        .header("Authorization", "Bearer " + secret))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAdminsCanManageTokensAndInvalidSecretsInspectSafely() throws Exception {
        String closerJwt = login("closer@demo.doxa.com");
        mockMvc.perform(post("/api/developer/tokens")
                        .header("Authorization", "Bearer " + closerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"External app","scopes":["contacts:read"],"expiresAt":null}
                                """))
                .andExpect(status().isForbidden());

        String adminJwt = login("admin@demo.doxa.com");
        MvcResult result = mockMvc.perform(post("/api/developer/tokens/inspect")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"not-a-real-token"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(jsonMapper.readTree(result.getResponse().getContentAsString()).path("status").asText())
                .isEqualTo("INVALID");

        JsonNode created = createToken(adminJwt, "[\"contacts:read\"]");
        String secret = created.path("secret").asText();
        var stored = tokenRepository.findByTokenPrefix(secret.substring(0, 20)).orElseThrow();
        stored.setExpiresAt(java.time.Instant.now().minusSeconds(1));
        tokenRepository.save(stored);

        MvcResult expiredResult = mockMvc.perform(post("/api/developer/tokens/inspect")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s"}
                                """.formatted(secret)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(jsonMapper.readTree(expiredResult.getResponse().getContentAsString()).path("status").asText())
                .isEqualTo("EXPIRED");
        mockMvc.perform(get("/api/contacts").header("Authorization", "Bearer " + secret))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mcpExposesOnlyToolsGrantedByTokenScopes() throws Exception {
        String adminJwt = login("admin@demo.doxa.com");
        JsonNode created = createToken(adminJwt, "[\"mcp:access\",\"contacts:read\",\"opportunities:write\"]");
        String secret = created.path("secret").asText();

        MvcResult initialized = mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer " + secret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18"}}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode initializeResult = jsonMapper.readTree(initialized.getResponse().getContentAsString());
        assertThat(initializeResult.path("result").path("protocolVersion").asText()).isEqualTo("2025-06-18");
        assertThat(initialized.getResponse().getHeader("MCP-Protocol-Version")).isEqualTo("2025-06-18");

        MvcResult toolsResult = mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer " + secret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":2,"method":"tools/list"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tools = jsonMapper.readTree(toolsResult.getResponse().getContentAsString())
                .path("result").path("tools");
        assertThat(tools.size()).isEqualTo(2);
        assertThat(tools.get(0).path("name").asText()).isEqualTo("search_contacts");
        assertThat(tools.get(1).path("name").asText()).isEqualTo("update_opportunity_stage");

        MvcResult callResult = mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer " + secret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"search_contacts","arguments":{"query":"Demo"}}}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(jsonMapper.readTree(callResult.getResponse().getContentAsString())
                .path("result").path("structuredContent").path("content").size()).isEqualTo(1);

        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer " + secret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"update_opportunity_stage","arguments":{"opportunityId":"dddddddd-dddd-dddd-dddd-dddddddddddd","stageSlug":"new-lead"}}}
                                """))
                .andExpect(status().isOk());
    }

    private JsonNode createToken(String adminJwt, String scopes) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/developer/tokens")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Portal test","scopes":%s,"expiresAt":null}
                                """.formatted(scopes)))
                .andExpect(status().isCreated())
                .andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }
}
