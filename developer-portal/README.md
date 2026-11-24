# DOXA Developer Portal

Independent Next.js application for CRM API documentation, endpoint testing, administrator-managed API tokens, and MCP configuration.

## Run

```powershell
npm install
npm run dev
```

By default the portal runs at `http://localhost:3001`. Set `DEVELOPER_PORTAL_PORT` to choose another port. Configure `NEXT_PUBLIC_API_URL` to point to the existing Spring Boot API (default `http://localhost:8080`); `NEXT_PUBLIC_MCP_URL` overrides the URL shown in MCP setup.

Sign in with the same CRM account. Token management requires the `ADMIN` role. API token secrets are shown once and stored as BCrypt hashes.

The MCP server uses stateless Streamable HTTP at `/mcp` on the existing backend. Tokens need `mcp:access` in addition to the data scopes needed by each tool or resource.
