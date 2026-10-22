package com.configvault.config;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.configvault.crypto.EncryptedValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ConfigEntryRepository {

    private static final String ENTRY_SELECT = """
            SELECT e.id, e.environment_id, e.service_id, e.key, e.is_secret,
                   v.version_no, v.value_ciphertext, v.nonce, v.key_id, v.created_at
            FROM config_entries e
            JOIN config_versions v
              ON v.id = e.current_version_id AND v.entry_id = e.id
            """;

    private static final RowMapper<ConfigEntrySnapshot> ENTRY_MAPPER = (result, rowNum) ->
            new ConfigEntrySnapshot(
                    result.getObject("id", UUID.class),
                    result.getObject("environment_id", UUID.class),
                    result.getObject("service_id", UUID.class),
                    result.getString("key"),
                    result.getBoolean("is_secret"),
                    result.getInt("version_no"),
                    result.getBytes("value_ciphertext"),
                    result.getBytes("nonce"),
                    result.getString("key_id"),
                    result.getTimestamp("created_at").toInstant());

    private static final RowMapper<ConfigVersionSnapshot> VERSION_MAPPER = (result, rowNum) ->
            new ConfigVersionSnapshot(
                    result.getObject("id", UUID.class),
                    result.getInt("version_no"),
                    result.getBytes("value_ciphertext"),
                    result.getBytes("nonce"),
                    result.getString("key_id"),
                    result.getString("created_by"),
                    result.getTimestamp("created_at").toInstant());

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ConfigEntryRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void create(
            UUID entryId,
            UUID environmentId,
            UUID serviceId,
            String key,
            boolean secret,
            EncryptedValue value,
            String actor) {
        jdbc.update(
                """
                INSERT INTO config_entries (id, environment_id, service_id, key, is_secret)
                VALUES (?, ?, ?, ?, ?)
                """,
                entryId,
                environmentId,
                serviceId,
                key,
                secret);
        UUID versionId = appendVersion(entryId, 1, value, actor);
        jdbc.update(
                "UPDATE config_entries SET current_version_id = ? WHERE id = ?",
                versionId,
                entryId);
        writeAudit(actor, "CONFIG_CREATED", entryId, key);
    }

    public Optional<ConfigEntrySnapshot> find(
            UUID environmentId,
            UUID serviceId,
            String key) {
        return jdbc.query(
                        ENTRY_SELECT + " WHERE e.environment_id = ? AND e.service_id = ? AND e.key = ?",
                        ENTRY_MAPPER,
                        environmentId,
                        serviceId,
                        key)
                .stream()
                .findFirst();
    }

    public List<ConfigEntrySnapshot> findAll(UUID environmentId, UUID serviceId) {
        return jdbc.query(
                ENTRY_SELECT + " WHERE e.environment_id = ? AND e.service_id = ? ORDER BY e.key",
                ENTRY_MAPPER,
                environmentId,
                serviceId);
    }

    public void appendVersion(
            UUID entryId,
            String key,
            boolean secret,
            EncryptedValue value,
            String actor) {
        appendVersion(entryId, key, secret, value, actor, "CONFIG_UPDATED", null);
    }

    public void appendRollbackVersion(
            UUID entryId,
            String key,
            boolean secret,
            EncryptedValue value,
            String actor,
            int rolledBackToVersion) {
        appendVersion(
                entryId,
                key,
                secret,
                value,
                actor,
                "CONFIG_ROLLED_BACK",
                rolledBackToVersion);
    }

    private void appendVersion(
            UUID entryId,
            String key,
            boolean secret,
            EncryptedValue value,
            String actor,
            String auditAction,
            Integer rollbackVersion) {
        jdbc.queryForObject(
                "SELECT id FROM config_entries WHERE id = ? FOR UPDATE",
                UUID.class,
                entryId);
        Integer currentVersion = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version_no), 0) FROM config_versions WHERE entry_id = ?",
                Integer.class,
                entryId);
        int version = currentVersion + 1;
        UUID versionId = appendVersion(entryId, version, value, actor);
        jdbc.update(
                """
                UPDATE config_entries
                SET current_version_id = ?, is_secret = ?
                WHERE id = ?
                """,
                versionId,
                secret,
                entryId);
        writeAudit(actor, auditAction, entryId, key, rollbackVersion);
    }

    public List<ConfigVersionSnapshot> findVersions(UUID entryId) {
        return jdbc.query(
                """
                SELECT id, version_no, value_ciphertext, nonce, key_id, created_by, created_at
                FROM config_versions
                WHERE entry_id = ?
                ORDER BY version_no DESC
                """,
                VERSION_MAPPER,
                entryId);
    }

    public Optional<ConfigVersionSnapshot> findVersion(UUID entryId, int version) {
        return jdbc.query(
                        """
                        SELECT id, version_no, value_ciphertext, nonce, key_id, created_by, created_at
                        FROM config_versions
                        WHERE entry_id = ? AND version_no = ?
                        """,
                        VERSION_MAPPER,
                        entryId,
                        version)
                .stream()
                .findFirst();
    }

    private UUID appendVersion(
            UUID entryId,
            int version,
            EncryptedValue value,
            String actor) {
        UUID versionId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO config_versions
                    (id, entry_id, version_no, value_ciphertext, key_id, nonce, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                versionId,
                entryId,
                version,
                value.ciphertext(),
                value.keyId(),
                value.nonce(),
                actor);
        return versionId;
    }

    private void writeAudit(String actor, String action, UUID entryId, String key) {
        writeAudit(actor, action, entryId, key, null);
    }

    private void writeAudit(
            String actor,
            String action,
            UUID entryId,
            String key,
            Integer version) {
        String metadata;
        try {
            metadata = version == null
                    ? objectMapper.writeValueAsString(java.util.Map.of("key", key))
                    : objectMapper.writeValueAsString(
                            java.util.Map.of("key", key, "version", version));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to create config audit metadata", exception);
        }
        jdbc.update(
                """
                INSERT INTO audit_log (id, actor, action, target_type, target_id, metadata)
                VALUES (?, ?, ?, 'CONFIG_ENTRY', ?, ?::jsonb)
                """,
                UUID.randomUUID(),
                actor,
                action,
                entryId,
                metadata);
    }
}
