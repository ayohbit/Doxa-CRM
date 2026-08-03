-- Core multi-tenant CRM schema (DOXA test)

CREATE TABLE licenses (
    id              UUID PRIMARY KEY,
    company_name    VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    webhook_secret  VARCHAR(255) NOT NULL,
    plan            VARCHAR(50),
    lead_credit_balance NUMERIC(12, 2),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    license_id      UUID         NOT NULL REFERENCES licenses (id),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_license_email UNIQUE (license_id, email)
);

CREATE INDEX idx_users_license_id ON users (license_id);

CREATE TABLE pipelines (
    id              UUID PRIMARY KEY,
    license_id      UUID         NOT NULL REFERENCES licenses (id),
    name            VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_pipelines_license_name UNIQUE (license_id, name)
);

CREATE INDEX idx_pipelines_license_id ON pipelines (license_id);

CREATE TABLE stages (
    id              UUID PRIMARY KEY,
    pipeline_id     UUID         NOT NULL REFERENCES pipelines (id),
    slug            VARCHAR(100) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    position        INT          NOT NULL,
    monetary_value  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    CONSTRAINT uq_stages_pipeline_slug UNIQUE (pipeline_id, slug)
);

CREATE INDEX idx_stages_pipeline_id ON stages (pipeline_id);

CREATE TABLE contacts (
    id              UUID PRIMARY KEY,
    license_id      UUID         NOT NULL REFERENCES licenses (id),
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(50),
    phone_e164      VARCHAR(20),
    tags            JSONB        NOT NULL DEFAULT '[]'::jsonb,
    custom_fields   JSONB        NOT NULL DEFAULT '{}'::jsonb,
    dedupe_key      VARCHAR(512) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_contacts_license_dedupe UNIQUE (license_id, dedupe_key)
);

CREATE INDEX idx_contacts_license_id ON contacts (license_id);
CREATE INDEX idx_contacts_license_email ON contacts (license_id, email);

CREATE TABLE opportunities (
    id                  UUID PRIMARY KEY,
    license_id          UUID         NOT NULL REFERENCES licenses (id),
    contact_id          UUID         NOT NULL REFERENCES contacts (id),
    stage_id            UUID         NOT NULL REFERENCES stages (id),
    value               NUMERIC(12, 2) NOT NULL DEFAULT 0,
    currency            VARCHAR(3)   NOT NULL DEFAULT 'USD',
    ad_set              VARCHAR(255),
    revenue_monthly     VARCHAR(100),
    source              VARCHAR(50)  NOT NULL DEFAULT 'manual',
    broker_lead_id      VARCHAR(100),
    assigned_user_id    UUID         REFERENCES users (id),
    status              VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    lost_reason         VARCHAR(255),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_opportunities_license_id ON opportunities (license_id);
CREATE INDEX idx_opportunities_stage_id ON opportunities (stage_id);
CREATE INDEX idx_opportunities_contact_id ON opportunities (contact_id);
CREATE INDEX idx_opportunities_assigned_user ON opportunities (assigned_user_id);
CREATE UNIQUE INDEX uq_opportunities_broker_lead
    ON opportunities (license_id, broker_lead_id)
    WHERE broker_lead_id IS NOT NULL;

CREATE TABLE stage_history (
    id              UUID PRIMARY KEY,
    opportunity_id  UUID         NOT NULL REFERENCES opportunities (id),
    from_stage_id   UUID         REFERENCES stages (id),
    to_stage_id     UUID         NOT NULL REFERENCES stages (id),
    changed_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    changed_by      UUID         REFERENCES users (id)
);

CREATE INDEX idx_stage_history_opportunity ON stage_history (opportunity_id);
CREATE INDEX idx_stage_history_changed_at ON stage_history (changed_at);

CREATE TABLE webhook_logs (
    id              UUID PRIMARY KEY,
    license_id      UUID         REFERENCES licenses (id),
    raw_payload     JSONB        NOT NULL,
    signature_valid BOOLEAN      NOT NULL,
    result          VARCHAR(50)  NOT NULL,
    error_message   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhook_logs_license_id ON webhook_logs (license_id);

CREATE TABLE opportunity_calls (
    id              UUID PRIMARY KEY,
    opportunity_id  UUID         NOT NULL UNIQUE REFERENCES opportunities (id),
    fathom_url      VARCHAR(500),
    ai_score        NUMERIC(5, 2),
    ai_summary      TEXT,
    outcome         VARCHAR(50),
    objection       TEXT,
    next_step       TEXT,
    filled_by       UUID         REFERENCES users (id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
