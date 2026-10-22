CREATE TABLE services (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_services_project_name UNIQUE (project_id, name),
    CONSTRAINT uq_services_id_project UNIQUE (id, project_id)
);

CREATE TABLE environment_services (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    environment_id UUID NOT NULL,
    service_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_environment_services_pair UNIQUE (environment_id, service_id),
    CONSTRAINT uq_environment_services_id_pair UNIQUE (id, environment_id, service_id),
    CONSTRAINT fk_environment_services_environment
        FOREIGN KEY (environment_id, project_id)
        REFERENCES environments (id, project_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_environment_services_service
        FOREIGN KEY (service_id, project_id)
        REFERENCES services (id, project_id)
        ON DELETE CASCADE
);

ALTER TABLE config_entries
    DROP CONSTRAINT uq_config_entries_environment_key,
    ADD COLUMN service_id UUID;

ALTER TABLE config_entries
    ADD CONSTRAINT fk_config_entries_environment_service
        FOREIGN KEY (environment_id, service_id)
        REFERENCES environment_services (environment_id, service_id)
        ON DELETE CASCADE,
    ADD CONSTRAINT uq_config_entries_service_environment_key
        UNIQUE (environment_id, service_id, key);

ALTER TABLE api_tokens
    ADD COLUMN service_id UUID;

ALTER TABLE api_tokens
    ADD CONSTRAINT fk_api_tokens_environment_service
        FOREIGN KEY (environment_id, service_id)
        REFERENCES environment_services (environment_id, service_id)
        ON DELETE CASCADE;

CREATE INDEX idx_services_project_id ON services (project_id);
CREATE INDEX idx_environment_services_environment_id ON environment_services (environment_id);
CREATE INDEX idx_environment_services_service_id ON environment_services (service_id);
CREATE INDEX idx_config_entries_service_environment
    ON config_entries (environment_id, service_id);
CREATE INDEX idx_api_tokens_environment_service_active
    ON api_tokens (environment_id, service_id) WHERE revoked_at IS NULL;
