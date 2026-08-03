# DOXA CRM 1.0

CRM multi-tenant para licenciados DOXA — backend **Spring Boot 4** + **PostgreSQL** e frontend **Next.js 16**.  
Dashboard, pipeline Kanban, contatos, webhook Lead Broker e integrações (WhatsApp, Google, Fathom, Telegram).

---

## Pré-requisitos

| Ferramenta | Versão mínima |
|------------|---------------|
| **Java** | 21 |
| **Node.js** | 20+ |
| **Docker Desktop** | Para PostgreSQL local |
| **Git** | Clone do repositório |

---

## Passo a passo (do zero)

### 1. Clone o repositório

```bash
git clone https://github.com/ayohbit/Doxa-CRM.git
cd Doxa-CRM
```

### 2. Suba o banco de dados (PostgreSQL)

Abra um terminal na pasta `backend/`:

```bash
cd backend
docker compose up -d
```

Isso cria o PostgreSQL em `localhost:5432`:

| Campo | Valor |
|-------|-------|
| Database | `doxa_crm` |
| Usuário | `doxa` |
| Senha | `doxa` |

Verifique se o container está rodando:

```bash
docker compose ps
```

### 3. Suba o backend (API)

No mesmo terminal (`backend/`):

**Windows (PowerShell):**
```powershell
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**
```bash
./mvnw spring-boot:run
```

Na **primeira execução** com banco vazio:
- Flyway aplica as migrations (`V1`–`V4`)
- O seed cria tenants demo, usuários, pipeline, ~46 oportunidades e contatos

API disponível em: **http://localhost:8080**

Teste rápido:
```bash
curl http://localhost:8080/api/health
# {"status":"UP"}
```

### 4. Suba o frontend

Abra **outro terminal** na pasta `frontend/`:

```bash
cd frontend
npm install
npm run dev
```

Frontend em: **http://localhost:3000**

### 5. Faça login

| E-mail | Senha | Papel |
|--------|-------|-------|
| `admin@demo.doxa.com` | `password123` | Admin (acesso total) |
| `closer@demo.doxa.com` | `password123` | Closer (só suas opps) |
| `sdr@demo.doxa.com` | `password123` | SDR (estágios de triagem) |

---

## O que você pode fazer na UI

| Tela | Funcionalidades |
|------|-----------------|
| **Dashboard** (`/`) | KPIs reais, gráficos, filtros por período / Ad Set / time |
| **Opportunities** | Kanban + lista, busca, **Add opportunity**, ações (WhatsApp, e-mail, calendar, wrap-up, Fathom) |
| **Contacts** | Tabela com busca, **Add contact** |

---

## Scripts do backend (continuam funcionando)

### Testar webhook Lead Broker

Com o backend rodando:

**Windows:**
```powershell
cd backend
.\scripts\test-lead-broker-webhook.ps1
```

Resposta esperada: **201** (lead novo) ou **200** (idempotente, se `broker_lead_id` já existir).

Credenciais demo do webhook:
- `license_id`: `lic_demo`
- `webhook_secret`: `whsec_demo_license_secret_change_me`

### Testes automatizados

```bash
cd backend
./mvnw test
```

> **Nota:** alguns testes de integração exigem PostgreSQL ou perfil de teste configurado. O webhook e isolamento de tenant têm testes dedicados.

---

## Variáveis de ambiente (opcionais)

Copie e ajuste conforme necessário. O projeto **funciona sem elas** para o fluxo core (login, CRM, dashboard, webhook).

### Backend (`backend/` — env ou `application.properties`)

| Variável | Para quê | Obrigatório? |
|----------|----------|--------------|
| `JWT_SECRET` | Assinatura dos tokens JWT | Não (há default dev) |
| `GOOGLE_CLIENT_ID` | OAuth Google (Calendar + Gmail) | Só para integrações Google |
| `GOOGLE_CLIENT_SECRET` | OAuth Google | Só para integrações Google |
| `GOOGLE_REDIRECT_URI` | Callback OAuth (default: `http://localhost:8080/api/integrations/google/callback`) | Só se usar Google |
| `TELEGRAM_BOT_TOKEN` | Bot Telegram (@BotFather) | Só para alertas Telegram |
| `FATHOM_API_KEY` | API Fathom (análise de calls) | Não — modo demo sem chave |

### Frontend (`frontend/.env.local`)

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

---

## Observações importantes

### Integrações que precisam de configuração extra

1. **Google Calendar / Gmail**  
   - Crie um projeto no [Google Cloud Console](https://console.cloud.google.com/)  
   - Ative Calendar API e Gmail API  
   - Configure OAuth com redirect URI: `http://localhost:8080/api/integrations/google/callback`  
   - Defina `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET`  
   - No app, abra um modal de Calendar ou E-mail e clique em **Conectar Google**

2. **Telegram (alertas internos ao time)**  
   - Crie um bot via [@BotFather](https://t.me/BotFather)  
   - Defina `TELEGRAM_BOT_TOKEN`  
   - Insira o `chat_id` do grupo/canal na tabela `telegram_settings` para sua licença:
   ```sql
   INSERT INTO telegram_settings (id, license_id, chat_id, enabled)
   VALUES (
     gen_random_uuid(),
     (SELECT id FROM licenses WHERE broker_license_id = 'lic_demo'),
     '-1001234567890',
     true
   );
   ```
   Alertas disparam em: novo lead (webhook), no-show, oportunidade ganha.

3. **WhatsApp**  
   - **Não usa token de API** — abre link `wa.me` com telefone normalizado (E.164)  
   - Funciona automaticamente se o contato tiver telefone válido  
   - Ícone de mensagem nos cards de oportunidade

4. **Fathom**  
   - Cole a URL da gravação no modal de análise  
   - Sem `FATHOM_API_KEY`, roda análise **demo** com score simulado  
   - Com chave, tenta buscar transcrição real na API Fathom

5. **Ad Spend no Dashboard**  
   - Estimativa `leads × $158` (configurável) — não há integração com Meta Ads ainda

### Papéis e permissões

- **Closer** tem contatos **somente leitura** — não consegue usar **Add contact** (403)
- **Admin** e **SDR** podem criar contatos e oportunidades

### Ordem de execução

Sempre nesta ordem: **Docker (PostgreSQL) → Backend → Frontend**.  
Se o backend subir antes do banco, reinicie após o Postgres estar healthy.

---

## Estrutura do projeto

```
Doxa-CRM/
├── backend/          # API Spring Boot, Flyway, Docker Compose
│   ├── scripts/      # test-lead-broker-webhook.ps1
│   └── src/main/resources/db/migration/
├── frontend/         # Next.js 16 App Router
├── docs/API.md       # Referência de endpoints
└── WRITEUP.md        # Decisões técnicas e trade-offs
```

---

## Documentação adicional

- [Referência de API](docs/API.md)
- [WRITEUP — decisões de arquitetura](WRITEUP.md)
- [Backend — detalhes técnicos](backend/README.md)

---

## Licença / uso

Projeto desenvolvido como teste técnico DOXA CRM.
