# Referência de API — DOXA CRM

Base URL: `http://localhost:8080`

Autenticação: `Authorization: Bearer <token>` (exceto rotas públicas).

---

## Públicas

| Método | Endpoint |
|--------|----------|
| GET | `/api/health` |
| POST | `/api/auth/login` |
| POST | `/api/webhooks/lead-broker` |
| GET | `/api/integrations/google/callback` |

---

## Auth

| Método | Endpoint |
|--------|----------|
| POST | `/api/auth/logout` |
| GET | `/api/auth/me` |

---

## Dashboard

| Método | Endpoint | Query |
|--------|----------|-------|
| GET | `/api/dashboard/kpis` | `periodDays`, `assignedUserId?`, `adSet?` |
| GET | `/api/dashboard/daily-series` | idem |
| GET | `/api/dashboard/funnel` | idem |
| GET | `/api/dashboard/ad-sets` | — |
| GET | `/api/dashboard/team` | — |

---

## Opportunities

| Método | Endpoint |
|--------|----------|
| GET | `/api/opportunities` |
| GET | `/api/opportunities/{id}` |
| POST | `/api/opportunities` |
| PUT | `/api/opportunities/{id}` |
| PATCH | `/api/opportunities/{id}/stage` |
| DELETE | `/api/opportunities/{id}` |
| POST | `/api/opportunities/{id}/wrap-up` |
| POST | `/api/opportunities/{id}/call-analysis` |
| POST | `/api/opportunities/{id}/calendar/invite` |

**Criar oportunidade**
```json
{
  "contactId": "uuid",
  "stageSlug": "new-lead",
  "value": 45.00,
  "adSet": "Campaign",
  "revenueMonthly": "$10k - $25k/mo"
}
```

---

## Contacts

| Método | Endpoint |
|--------|----------|
| GET | `/api/contacts` |
| POST | `/api/contacts` |
| PUT | `/api/contacts/{id}` |
| DELETE | `/api/contacts/{id}` |
| POST | `/api/contacts/{id}/email/send` |
| GET | `/api/contacts/{id}/timeline` |

**Criar contato**
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "phone": "+14075551234",
  "tags": ["ads-lead"]
}
```

---

## Integrações

| Método | Endpoint |
|--------|----------|
| GET | `/api/integrations/status` |
| GET | `/api/integrations/google/auth-url` |

---

## API Tokens (Admin)

Todas as rotas abaixo exigem JWT de usuário Administrador.

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/developer/tokens/scopes` | Escopos suportados |
| GET | `/api/developer/tokens` | Tokens da licença |
| POST | `/api/developer/tokens` | Criar token; o segredo completo é retornado uma única vez |
| DELETE | `/api/developer/tokens/{id}` | Revogar token |
| POST | `/api/developer/tokens/inspect` | Validar e inspecionar token |

Os tokens usam o prefixo `doxa_live_`, são armazenados como hash BCrypt e aplicam escopos por endpoint. A revogação/expiração é verificada a cada requisição.

## Model Context Protocol

Endpoint Streamable HTTP: `POST /mcp`. Envie JSON-RPC 2.0 com o header `Authorization: Bearer <api-token>`. O token precisa do escopo `mcp:access`; cada Tool/Resource também verifica seu escopo de dados.

Métodos implementados: `initialize`, `ping`, `tools/list`, `tools/call`, `resources/list` e `resources/read`.

Tools: `search_contacts`, `list_opportunities`, `get_dashboard_kpis`. Resource: `crm://dashboard/overview`.

---

## Webhook

Header: `X-Broker-Signature: sha256=<HMAC do body>`

Demo: `license_id=lic_demo`, secret `whsec_demo_license_secret_change_me`

Script: `backend/scripts/test-lead-broker-webhook.ps1`
