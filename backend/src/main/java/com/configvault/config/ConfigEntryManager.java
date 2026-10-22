package com.configvault.config;

import java.util.List;
import java.util.UUID;

import com.configvault.common.ResourceNotFoundException;
import com.configvault.crypto.AesGcmEncryptionService;
import com.configvault.crypto.EncryptedValue;
import com.configvault.environment.EnvironmentServiceAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigEntryManager {

    private static final String LOCAL_ACTOR = "local-development";
    private static final String MASKED_VALUE = "********";

    private final EnvironmentServiceAssignmentRepository assignments;
    private final ConfigEntryRepository entries;
    private final AesGcmEncryptionService encryption;

    public ConfigEntryManager(
            EnvironmentServiceAssignmentRepository assignments,
            ConfigEntryRepository entries,
            AesGcmEncryptionService encryption) {
        this.assignments = assignments;
        this.entries = entries;
        this.encryption = encryption;
    }

    @Transactional
    public ConfigEntryResponse create(
            UUID projectId,
            UUID environmentId,
            UUID serviceId,
            ConfigEntryCreateRequest request) {
        requireAssignment(projectId, environmentId, serviceId);
        UUID entryId = UUID.randomUUID();
        entries.create(
                entryId,
                environmentId,
                serviceId,
                request.key(),
                request.secret(),
                encryption.encrypt(request.value()),
                LOCAL_ACTOR);
        return get(projectId, environmentId, serviceId, request.key());
    }

    @Transactional(readOnly = true)
    public List<ConfigEntryResponse> list(UUID projectId, UUID environmentId, UUID serviceId) {
        requireAssignment(projectId, environmentId, serviceId);
        return entries.findAll(environmentId, serviceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConfigEntryResponse get(
            UUID projectId,
            UUID environmentId,
            UUID serviceId,
            String key) {
        requireAssignment(projectId, environmentId, serviceId);
        return toResponse(findEntry(environmentId, serviceId, key));
    }

    @Transactional
    public ConfigEntryResponse update(
            UUID projectId,
            UUID environmentId,
            UUID serviceId,
            String key,
            ConfigEntryUpdateRequest request) {
        requireAssignment(projectId, environmentId, serviceId);
        ConfigEntrySnapshot entry = findEntry(environmentId, serviceId, key);
        entries.appendVersion(
                entry.id(),
                key,
                entry.secret(),
                encryption.encrypt(request.value()),
                LOCAL_ACTOR);
        return get(projectId, environmentId, serviceId, key);
    }

    @Transactional(readOnly = true)
    public List<ConfigVersionResponse> history(
            UUID projectId,
            UUID environmentId,
            UUID serviceId,
            String key) {
        requireAssignment(projectId, environmentId, serviceId);
        ConfigEntrySnapshot entry = findEntry(environmentId, serviceId, key);
        return entries.findVersions(entry.id()).stream()
                .map(ConfigVersionResponse::from)
                .toList();
    }

    @Transactional
    public ConfigEntryResponse rollback(
            UUID projectId,
            UUID environmentId,
            UUID serviceId,
            String key,
            int version) {
        requireAssignment(projectId, environmentId, serviceId);
        ConfigEntrySnapshot entry = findEntry(environmentId, serviceId, key);
        ConfigVersionSnapshot previous = entries.findVersion(entry.id(), version)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Config version", key + "/" + version));
        String previousValue = encryption.decrypt(
                previous.ciphertext(),
                previous.nonce(),
                previous.keyId());
        entries.appendRollbackVersion(
                entry.id(),
                key,
                entry.secret(),
                encryption.encrypt(previousValue),
                LOCAL_ACTOR,
                version);
        return get(projectId, environmentId, serviceId, key);
    }

    private ConfigEntryResponse toResponse(ConfigEntrySnapshot entry) {
        String value = entry.secret()
                ? MASKED_VALUE
                : encryption.decrypt(entry.ciphertext(), entry.nonce(), entry.keyId());
        return ConfigEntryResponse.from(entry, value);
    }

    private ConfigEntrySnapshot findEntry(UUID environmentId, UUID serviceId, String key) {
        return entries.find(environmentId, serviceId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Config entry", key));
    }

    private void requireAssignment(UUID projectId, UUID environmentId, UUID serviceId) {
        if (!assignments.existsByProjectIdAndEnvironmentIdAndServiceId(
                projectId, environmentId, serviceId)) {
            throw new ResourceNotFoundException(
                    "Service assignment", environmentId + "/" + serviceId);
        }
    }
}
