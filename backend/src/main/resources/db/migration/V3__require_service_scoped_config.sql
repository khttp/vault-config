DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM config_entries WHERE service_id IS NULL) THEN
        RAISE EXCEPTION
            'Cannot require service-scoped config while unassigned config entries exist; assign or remove them first';
    END IF;

    IF EXISTS (SELECT 1 FROM api_tokens WHERE service_id IS NULL) THEN
        RAISE EXCEPTION
            'Cannot require service-scoped tokens while unassigned API tokens exist; recreate them with a service assignment';
    END IF;
END;
$$;

ALTER TABLE config_entries
    ALTER COLUMN service_id SET NOT NULL;

ALTER TABLE api_tokens
    ALTER COLUMN service_id SET NOT NULL;
