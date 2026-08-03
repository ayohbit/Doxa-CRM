CREATE TABLE oauth_connections (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL UNIQUE REFERENCES users (id),
    provider        VARCHAR(30)  NOT NULL DEFAULT 'google',
    access_token    TEXT         NOT NULL,
    refresh_token   TEXT,
    expires_at      TIMESTAMPTZ,
    scopes          TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE contact_timeline_events (
    id              UUID PRIMARY KEY,
    contact_id      UUID         NOT NULL REFERENCES contacts (id),
    license_id      UUID         NOT NULL REFERENCES licenses (id),
    event_type      VARCHAR(50)  NOT NULL,
    title           VARCHAR(255) NOT NULL,
    body            TEXT,
    metadata        JSONB        NOT NULL DEFAULT '{}',
    created_by      UUID         REFERENCES users (id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_timeline_contact ON contact_timeline_events (contact_id, created_at DESC);

CREATE TABLE telegram_settings (
    id              UUID PRIMARY KEY,
    license_id      UUID         NOT NULL UNIQUE REFERENCES licenses (id),
    chat_id         VARCHAR(100) NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
