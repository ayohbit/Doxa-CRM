-- Other tenant pipeline + stage (for isolation tests)
INSERT INTO pipelines (id, license_id, name, created_at)
SELECT
    '88888888-8888-8888-8888-888888888888',
    '22222222-2222-2222-2222-222222222222',
    'Ads Pipeline',
    NOW()
WHERE EXISTS (SELECT 1 FROM licenses WHERE id = '22222222-2222-2222-2222-222222222222')
  AND NOT EXISTS (
    SELECT 1 FROM pipelines WHERE license_id = '22222222-2222-2222-2222-222222222222'
);

INSERT INTO stages (id, pipeline_id, slug, name, position, monetary_value)
SELECT
    '99999999-9999-9999-9999-999999999999',
    '88888888-8888-8888-8888-888888888888',
    'new-lead',
    'New Lead',
    0,
    0
WHERE EXISTS (SELECT 1 FROM pipelines WHERE id = '88888888-8888-8888-8888-888888888888')
  AND NOT EXISTS (
    SELECT 1 FROM stages WHERE id = '99999999-9999-9999-9999-999999999999'
);

-- Dashboard demo: one closed deal for KPI/chart realism
UPDATE opportunities
SET status = 'WON',
    value = 5700,
    updated_at = NOW()
WHERE id = (
    SELECT o.id
    FROM opportunities o
    WHERE o.license_id = '11111111-1111-1111-1111-111111111111'
    ORDER BY o.created_at
    LIMIT 1
)
AND status <> 'WON';

-- Isolation test tenant: sample contact + opportunity (other license)
INSERT INTO contacts (id, license_id, name, email, phone, phone_e164, tags, custom_fields, dedupe_key, created_at, updated_at)
SELECT
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '22222222-2222-2222-2222-222222222222',
    'Other Tenant Contact',
    'secret@other-tenant.com',
    '+1 (555) 000-0001',
    '+15550000001',
    '[]'::jsonb,
    '{}'::jsonb,
    'email:secret@other-tenant.com',
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM licenses WHERE id = '22222222-2222-2222-2222-222222222222')
  AND NOT EXISTS (
    SELECT 1 FROM contacts WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
);

INSERT INTO opportunities (
    id, license_id, contact_id, stage_id, value, currency, source, status, created_at, updated_at
)
SELECT
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    '22222222-2222-2222-2222-222222222222',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '99999999-9999-9999-9999-999999999999',
    9999,
    'USD',
    'MANUAL',
    'OPEN',
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM contacts WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')
  AND NOT EXISTS (
    SELECT 1 FROM opportunities WHERE id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
);
