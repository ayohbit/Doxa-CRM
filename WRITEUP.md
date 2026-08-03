# WRITEUP — DOXA CRM

Documento de decisões técnicas, trade-offs e ideias para o teste backend DOXA.

---

## 1. Stack escolhida

| Camada | Escolha | Motivo |
|--------|---------|--------|
| Backend | Java 21 + Spring Boot 4.1 | Robustez, JPA, Security, ecossistema maduro |
| Banco | PostgreSQL 16 | JSONB, constraints, multi-tenant |
| Migrations | Flyway | Versionamento explícito do schema |
| Auth | JWT stateless | Simples para SPA Next.js |
| Frontend | Next.js 16 (App Router) | Base fornecida no teste; mínimas alterações visuais |

Alternativa considerada: Node + NestJS + Prisma. Descartada para demonstrar fluência em JVM e porque o frontend já era React/Next.

---

## 2. Modelo de dados

### Multi-tenant via `licenses`

Todo registro de negócio carrega `license_id`. Isolamento aplicado em **Specifications JPA** + teste automatizado (`TenantIsolationTest`).

### `dedupe_key` em contatos

Chave derivada de e-mail normalizado ou telefone E.164 (`email:…` / `phone:…`), única por licença. Simplifica deduplicação do webhook sem triggers no banco.

### `stage_history` obrigatório

Cada movimentação de estágio grava histórico. Dashboard de funil e drop-off usa **estágio atual** + histórico para contagem temporal.

### `opportunity_calls` (1:1)

Wrap-up manual e análise Fathom compartilham a mesma entidade — evita duplicar formulários e scores.

### `contact_timeline_events`

Auditoria de integrações (e-mail, calendar, wrap-up, Fathom) separada do stage history — atende requisito de timeline do contato sem poluir oportunidades.

---

## 3. Webhook Lead Broker

- **Assinatura:** HMAC SHA-256 do corpo bruto (`X-Broker-Signature: sha256=…`)
- **Idempotência:** lookup por `(license_id, broker_lead_id)` antes de insert
- **Endpoint público** com `permitAll` + JWT filter skip
- **Erros 4xx** via `WebhookRejectedException` + log em `webhook_logs`
- Script PowerShell em `backend/scripts/test-lead-broker-webhook.ps1` permanece funcional

---

## 4. Papéis (Admin / Closer / SDR)

Implementados como **filtros em Specification**, não motor de permissões genérico (conforme sugerido no PDF).

| Papel | Regra principal |
|-------|-----------------|
| Admin | Tudo no tenant |
| Closer | Oportunidades atribuídas a ele; contatos read-only |
| SDR | Oportunidades em estágios de triagem (`SDR_STAGE_SLUGS`) |

---

## 5. Dashboard

KPIs de `opportunities` + `stage_history`. Ad Spend estimado (`leads × $158`) — sem integração real com ads.

---

## 6. Integrações

| Integração | Abordagem |
|------------|-----------|
| WhatsApp | Link `wa.me` — E.164 no backend |
| Google Calendar/Gmail | OAuth2 por usuário; HttpClient nativo |
| Fathom | Cliente HTTP + fallback demo sem API key |
| Telegram | Bot token + `chat_id` por licença |

---

## 7. Frontend

- Removido `lib/data.ts` — 100% API
- Modais Add Contact / Add Opportunity
- Toggle Kanban / Lista + sort na tela de Opportunities

---

## 8. O que faria diferente com mais tempo

1. OpenAPI gerado automaticamente (Springdoc)
2. Testcontainers no CI
3. Filas para Fathom assíncrono
4. Criptografia de tokens OAuth at rest

---

## 9. Ideias bônus

**Lead scoring automático** — score 0–100 por revenue, estágio e wrap-up.  
**Dashboard semanal via Telegram** — snapshot KPIs por tenant.

---

## 10. Non-negotiables (PDF §8)

| Requisito | Status |
|-----------|--------|
| Isolamento entre tenants | OK |
| Senhas bcrypt | OK |
| Migrations + seed | OK |
| Idempotência webhook testada | OK |
| Listas paginadas | OK |
| README setup rápido | OK |
