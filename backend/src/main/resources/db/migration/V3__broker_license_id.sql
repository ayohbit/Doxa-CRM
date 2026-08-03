ALTER TABLE licenses ADD COLUMN broker_license_id VARCHAR(100);

CREATE UNIQUE INDEX uq_licenses_broker_license_id
    ON licenses (broker_license_id)
    WHERE broker_license_id IS NOT NULL;

UPDATE licenses
SET broker_license_id = 'lic_demo'
WHERE id = '11111111-1111-1111-1111-111111111111';

UPDATE licenses
SET broker_license_id = 'lic_other'
WHERE id = '22222222-2222-2222-2222-222222222222';
