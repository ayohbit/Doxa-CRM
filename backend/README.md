# DOXA CRM — Backend

API Spring Boot 21 + PostgreSQL para o CRM multi-tenant.

## Pré-requisitos

- Java 21
- Docker Desktop (PostgreSQL local)
- Maven (ou use `./mvnw`)

## Subir o banco local

Na pasta `backend/`:

```bash
docker compose up -d
```

Isso cria o PostgreSQL em `localhost:5432` com:

| Campo    | Valor      |
|----------|------------|
| Database | `doxa_crm` |
| User     | `doxa`     |
| Password | `doxa`     |

A configuração está em `src/main/resources/application.properties`.

## Rodar a API

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

- Health: `GET /api/health`
- Login: `POST /api/auth/login`
- Usuário logado: `GET /api/auth/me` (header `Authorization: Bearer <token>`)

## Usuários demo (seed automático)

Na primeira execução com banco vazio, o seed cria dados equivalentes ao mock do frontend.

| Email               | Senha        | Papel  |
|---------------------|--------------|--------|
| admin@demo.doxa.com | password123  | ADMIN  |
| closer@demo.doxa.com| password123  | CLOSER |
| sdr@demo.doxa.com   | password123  | SDR    |

Também existe um segundo tenant (`admin@other.doxa.com`) para testes de isolamento futuros.

## Migrations

Schema gerenciado pelo Flyway em `src/main/resources/db/migration/`.

## Endpoints principais (etapa 2)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/pipelines/board` | Estágios do pipeline + totais |
| GET | `/api/opportunities?stageSlug=&q=&page=&size=` | Lista paginada |
| POST | `/api/opportunities` | Criar oportunidade |
| PUT | `/api/opportunities/{id}` | Atualizar |
| PATCH | `/api/opportunities/{id}/stage` | Mover estágio (grava history) |
| DELETE | `/api/opportunities/{id}` | Remover |
| GET | `/api/contacts?q=&page=&size=` | Lista paginada com busca |
| POST | `/api/contacts` | Criar contato |
| PUT | `/api/contacts/{id}` | Atualizar |
| DELETE | `/api/contacts/{id}` | Remover |

Todas as rotas (exceto health, login e webhooks) exigem header `Authorization: Bearer <token>`.

## Testes

```bash
./mvnw test
```

## Variáveis de ambiente (opcional)

| Variável     | Descrição                          |
|--------------|------------------------------------|
| `JWT_SECRET` | Secret HMAC para tokens JWT        |
