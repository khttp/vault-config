CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE environments (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_environments_project_name UNIQUE (project_id, name),
    CONSTRAINT uq_environments_id_project UNIQUE (id, project_id)
);

CREATE TABLE config_entries (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL REFERENCES environments (id) ON DELETE CASCADE,
    key VARCHAR(255) NOT NULL,
    is_secret BOOLEAN NOT NULL DEFAULT FALSE,
    current_version_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_config_entries_environment_key UNIQUE (environment_id, key)
);

CREATE TABLE config_versions (
    id UUID PRIMARY KEY,
    entry_id UUID NOT NULL REFERENCES config_entries (id) ON DELETE CASCADE,
    version_no INTEGER NOT NULL CHECK (version_no > 0),
    value_ciphertext BYTEA NOT NULL,
    key_id VARCHAR(120) NOT NULL,
    nonce BYTEA NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_config_versions_entry_version UNIQUE (entry_id, version_no),
    CONSTRAINT uq_config_versions_id_entry UNIQUE (id, entry_id)
);

ALTER TABLE config_entries
    ADD CONSTRAINT fk_config_entries_current_version
    FOREIGN KEY (current_version_id, id)
    REFERENCES config_versions (id, entry_id)
    DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE api_tokens (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    environment_id UUID NOT NULL,
    token_hash BYTEA NOT NULL UNIQUE,
    scope VARCHAR(16) NOT NULL DEFAULT 'read' CHECK (scope = 'read'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT fk_api_tokens_environment_project
        FOREIGN KEY (environment_id, project_id)
        REFERENCES environments (id, project_id)
        ON DELETE CASCADE
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(120) NOT NULL,
    target_type VARCHAR(120) NOT NULL,
    target_id UUID,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_environments_project_id ON environments (project_id);
CREATE INDEX idx_config_entries_environment_id ON config_entries (environment_id);
CREATE INDEX idx_config_versions_entry_created_at ON config_versions (entry_id, created_at DESC);
CREATE INDEX idx_api_tokens_environment_active ON api_tokens (environment_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at DESC);
