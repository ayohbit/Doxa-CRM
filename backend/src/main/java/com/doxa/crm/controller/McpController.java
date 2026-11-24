package com.doxa.crm.controller;

import com.doxa.crm.domain.enums.OpportunityStatus;
import com.doxa.crm.dto.dashboard.DashboardFilter;
import com.doxa.crm.dto.opportunity.MoveStageRequest;
import com.doxa.crm.exception.AccessDeniedException;
import com.doxa.crm.exception.ResourceNotFoundException;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.service.ContactService;
import com.doxa.crm.service.DashboardService;
import com.doxa.crm.service.OpportunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class McpController {

    private static final String PROTOCOL_VERSION = "2025-03-26";
    private final ContactService contactService;
    private final OpportunityService opportunityService;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/mcp", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> handle(
            @AuthenticationPrincipal AuthUser user,
            @RequestBody JsonNode request,
            @RequestHeader(value = "MCP-Protocol-Version", required = false) String protocolVersionHeader
    ) {
        if (!user.isApiToken()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(error(request.path("id"), -32001, "MCP requires an API token"));
        }
        JsonNode id = request.get("id");
        if (id == null || id.isNull()) {
            return ResponseEntity.accepted().build();
        }
        if (!"2.0".equals(request.path("jsonrpc").asText())) {
            return ResponseEntity.badRequest().body(error(id, -32600, "Invalid JSON-RPC request"));
        }

        String method = request.path("method").asText();
        JsonNode params = request.path("params");
        String responseProtocolVersion = responseProtocolVersion(method, params, protocolVersionHeader);
        try {
            Object result = switch (method) {
                case "initialize" -> initialize(params);
                case "ping" -> Map.of();
                case "tools/list" -> Map.of("tools", toolsFor(user));
                case "resources/list" -> Map.of("resources", resourcesFor(user));
                case "resources/read" -> readResource(user, params.path("uri").asText());
                case "tools/call" -> callTool(user, params);
                default -> throw new McpProtocolException(-32601, "Method not found");
            };
            return ResponseEntity.ok()
                    .header("MCP-Protocol-Version", responseProtocolVersion)
                    .body(success(id, result));
        } catch (McpProtocolException ex) {
            return ResponseEntity.ok()
                    .header("MCP-Protocol-Version", responseProtocolVersion)
                    .body(error(id, ex.code, ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.ok()
                    .header("MCP-Protocol-Version", responseProtocolVersion)
                    .body(error(id, -32602, ex.getMessage()));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.ok()
                    .header("MCP-Protocol-Version", responseProtocolVersion)
                    .body(error(id, -32003, ex.getMessage()));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.ok()
                    .header("MCP-Protocol-Version", responseProtocolVersion)
                    .body(error(id, -32004, ex.getMessage()));
        }
    }

    @GetMapping(value = "/mcp", produces = "text/event-stream")
    public ResponseEntity<String> eventStream(@AuthenticationPrincipal AuthUser user) {
        if (!user.isApiToken()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header("Allow", "POST")
                .body("Streamable HTTP requests should use POST /mcp");
    }

    private Map<String, Object> initialize(JsonNode params) {
        String clientVersion = params.path("protocolVersion").asText(PROTOCOL_VERSION);
        if (!clientVersion.equals("2025-03-26") && !clientVersion.equals("2025-06-18")) {
            throw new McpProtocolException(-32602, "Unsupported MCP protocol version");
        }
        return Map.of(
                "protocolVersion", clientVersion,
                "capabilities", Map.of(
                        "tools", Map.of(),
                        "resources", Map.of("subscribe", false, "listChanged", false)
                ),
                "serverInfo", Map.of("name", "doxa-crm-mcp", "version", "1.0.0")
        );
    }

    private String responseProtocolVersion(String method, JsonNode params, String header) {
        String requested = "initialize".equals(method)
                ? params.path("protocolVersion").asText(PROTOCOL_VERSION)
                : header;
        if ("2025-06-18".equals(requested)) {
            return requested;
        }
        return PROTOCOL_VERSION;
    }

    private List<Map<String, Object>> toolsFor(AuthUser user) {
        List<Map<String, Object>> tools = new ArrayList<>();
        if (user.getApiScopes().contains("contacts:read")) {
            tools.add(tool(
                    "search_contacts",
                    "Search contacts visible to the authenticated user.",
                    Map.of(
                            "type", "object",
                            "properties", Map.of("query", Map.of("type", "string", "description", "Name, email or phone query")),
                            "additionalProperties", false
                    )
            ));
        }
        if (user.getApiScopes().contains("opportunities:read")) {
            tools.add(tool(
                    "list_opportunities",
                    "List opportunities visible to the authenticated user.",
                    Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "query", Map.of("type", "string"),
                                    "stageSlug", Map.of("type", "string"),
                                    "status", Map.of("type", "string", "enum", List.of("OPEN", "WON", "LOST"))
                            ),
                            "additionalProperties", false
                    )
            ));
        }
        if (user.getApiScopes().contains("opportunities:write")) {
            tools.add(tool(
                    "update_opportunity_stage",
                    "Move an opportunity to a different stage in the authenticated license.",
                    Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "opportunityId", Map.of("type", "string", "format", "uuid"),
                                    "stageSlug", Map.of("type", "string")
                            ),
                            "required", List.of("opportunityId", "stageSlug"),
                            "additionalProperties", false
                    )
            ));
        }
        if (user.getApiScopes().contains("dashboard:read")) {
            tools.add(tool(
                    "get_dashboard_kpis",
                    "Get dashboard KPIs for a period of up to 365 days.",
                    Map.of(
                            "type", "object",
                            "properties", Map.of("periodDays", Map.of("type", "integer", "minimum", 1, "maximum", 365)),
                            "additionalProperties", false
                    )
            ));
        }
        return tools;
    }

    private List<Map<String, Object>> resourcesFor(AuthUser user) {
        if (!user.getApiScopes().contains("dashboard:read")) {
            return List.of();
        }
        return List.of(Map.of(
                "uri", "crm://dashboard/overview",
                "name", "CRM dashboard overview",
                "description", "Current aggregate CRM metrics for the last 30 days.",
                "mimeType", "application/json"
        ));
    }

    private Map<String, Object> readResource(AuthUser user, String uri) {
        if (!"crm://dashboard/overview".equals(uri)
                || !user.getApiScopes().contains("dashboard:read")) {
            throw new McpProtocolException(-32002, "Resource not found or scope not granted");
        }
        DashboardFilter filter = dashboardService.resolveFilter(user, 30, null, null);
        Object data = dashboardService.getKpis(user, filter);
        return Map.of("contents", List.of(Map.of(
                "uri", uri,
                "mimeType", "application/json",
                "text", toJson(data)
        )));
    }

    private Map<String, Object> callTool(AuthUser user, JsonNode params) {
        String name = params.path("name").asText();
        JsonNode args = params.path("arguments");
        Object data = switch (name) {
            case "search_contacts" -> {
                requireScope(user, "contacts:read");
                yield contactService.list(user, nullableText(args, "query"), 0, 20);
            }
            case "list_opportunities" -> {
                requireScope(user, "opportunities:read");
                OpportunityStatus status = args.hasNonNull("status")
                        ? OpportunityStatus.valueOf(args.path("status").asText())
                        : OpportunityStatus.OPEN;
                yield opportunityService.list(
                        user,
                        nullableText(args, "stageSlug"),
                        nullableText(args, "query"),
                        status,
                        0,
                        20
                );
            }
            case "update_opportunity_stage" -> {
                requireScope(user, "opportunities:write");
                String opportunityId = args.path("opportunityId").asText();
                String stageSlug = args.path("stageSlug").asText();
                if (opportunityId.isBlank() || stageSlug.isBlank()) {
                    throw new IllegalArgumentException("opportunityId and stageSlug are required");
                }
                yield opportunityService.moveStage(
                        user,
                        UUID.fromString(opportunityId),
                        new MoveStageRequest(stageSlug)
                );
            }
            case "get_dashboard_kpis" -> {
                requireScope(user, "dashboard:read");
                int periodDays = args.path("periodDays").asInt(30);
                if (periodDays < 1 || periodDays > 365) {
                    throw new IllegalArgumentException("periodDays must be between 1 and 365");
                }
                DashboardFilter filter = dashboardService.resolveFilter(user, periodDays, null, null);
                yield dashboardService.getKpis(user, filter);
            }
            default -> throw new McpProtocolException(-32602, "Unknown tool");
        };
        return Map.of(
                "content", List.of(Map.of("type", "text", "text", toJson(data))),
                "structuredContent", objectMapper.valueToTree(data)
        );
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> schema) {
        return Map.of("name", name, "description", description, "inputSchema", schema);
    }

    private void requireScope(AuthUser user, String scope) {
        if (!user.getApiScopes().contains(scope)) {
            throw new McpProtocolException(-32003, "Required API token scope is missing: " + scope);
        }
    }

    private String nullableText(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asText() : null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Could not serialize MCP response", ex);
        }
    }

    private Map<String, Object> success(JsonNode id, Object result) {
        return Map.of("jsonrpc", "2.0", "id", id, "result", result);
    }

    private Map<String, Object> error(JsonNode id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id == null || id.isNull() ? null : id);
        response.put("error", Map.of("code", code, "message", message));
        return response;
    }

    private static final class McpProtocolException extends RuntimeException {
        private final int code;

        private McpProtocolException(int code, String message) {
            super(message);
            this.code = code;
        }
    }
}
