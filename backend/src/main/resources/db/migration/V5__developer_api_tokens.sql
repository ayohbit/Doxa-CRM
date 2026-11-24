CREATE TABLE api_access_tokens (
    id                  UUID PRIMARY KEY,
    license_id          UUID NOT NULL REFERENCES licenses (id),
    created_by_user_id  UUID NOT NULL REFERENCES users (id),
    name                VARCHAR(120) NOT NULL,
    token_prefix        VARCHAR(32) NOT NULL UNIQUE,
    token_hash          VARCHAR(100) NOT NULL,
    scopes              JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMPTZ,
    last_used_at        TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ
);

CREATE INDEX idx_api_access_tokens_license_id ON api_access_tokens (license_id);
