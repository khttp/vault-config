package com.configvault.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.configvault.common.ResourceNotFoundException;
import com.configvault.crypto.AesGcmEncryptionService;
import com.configvault.crypto.EncryptedValue;
import com.configvault.environment.EnvironmentServiceAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigEntryManagerTest {

    private static final String TEST_KEY =
            java.util.Base64.getEncoder().encodeToString(new byte[32]);

    @Mock
    private EnvironmentServiceAssignmentRepository assignments;

    @Mock
    private ConfigEntryRepository entries;

    private AesGcmEncryptionService encryption;
    private ConfigEntryManager manager;

    @BeforeEach
    void setUp() {
        encryption = new AesGcmEncryptionService(TEST_KEY, "test-v1");
        manager = new ConfigEntryManager(assignments, entries, encryption);
    }

    @Test
    void encryptsNewValueBeforePersistingAndMasksSecretResponse() {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        when(assignments.existsByProjectIdAndEnvironmentIdAndServiceId(
                projectId, environmentId, serviceId)).thenReturn(true);
        when(entries.find(environmentId, serviceId, "API_KEY"))
                .thenReturn(Optional.of(snapshot(
                        entryId, environmentId, serviceId, "API_KEY", true, 1, "secret-value")));

        ConfigEntryResponse response = manager.create(
                projectId,
                environmentId,
                serviceId,
                new ConfigEntryCreateRequest("API_KEY", "secret-value", true));

        ArgumentCaptor<EncryptedValue> encryptedValue = ArgumentCaptor.forClass(EncryptedValue.class);
        verify(entries).create(
                any(UUID.class),
                org.mockito.ArgumentMatchers.eq(environmentId),
                org.mockito.ArgumentMatchers.eq(serviceId),
                org.mockito.ArgumentMatchers.eq("API_KEY"),
                org.mockito.ArgumentMatchers.eq(true),
                encryptedValue.capture(),
                org.mockito.ArgumentMatchers.eq("local-development"));
        assertEquals("secret-value", encryption.decrypt(
                encryptedValue.getValue().ciphertext(),
                encryptedValue.getValue().nonce(),
                encryptedValue.getValue().keyId()));
        assertFalse(java.util.Arrays.equals(
                "secret-value".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                encryptedValue.getValue().ciphertext()));
        assertEquals("********", response.value());
    }

    @Test
    void returnsDecryptedValueForNonSecretEntry() {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(assignments.existsByProjectIdAndEnvironmentIdAndServiceId(
                projectId, environmentId, serviceId)).thenReturn(true);
        when(entries.findAll(environmentId, serviceId))
                .thenReturn(List.of(snapshot(
                        UUID.randomUUID(),
                        environmentId,
                        serviceId,
                        "LOG_LEVEL",
                        false,
                        1,
                        "debug")));

        List<ConfigEntryResponse> response = manager.list(projectId, environmentId, serviceId);

        assertEquals("debug", response.getFirst().value());
        assertFalse(response.getFirst().secret());
    }

    @Test
    void appendsAnEncryptedVersionWhenUpdatingEntry() {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        when(assignments.existsByProjectIdAndEnvironmentIdAndServiceId(
                projectId, environmentId, serviceId)).thenReturn(true);
        when(entries.find(environmentId, serviceId, "API_KEY"))
                .thenReturn(
                        Optional.of(snapshot(
                                entryId, environmentId, serviceId, "API_KEY", true, 1, "old")),
                        Optional.of(snapshot(
                                entryId, environmentId, serviceId, "API_KEY", true, 2, "new")));

        ConfigEntryResponse response = manager.update(
                projectId,
                environmentId,
                serviceId,
                "API_KEY",
                new ConfigEntryUpdateRequest("new"));

        ArgumentCaptor<EncryptedValue> encryptedValue = ArgumentCaptor.forClass(EncryptedValue.class);
        verify(entries).appendVersion(
                org.mockito.ArgumentMatchers.eq(entryId),
                org.mockito.ArgumentMatchers.eq("API_KEY"),
                org.mockito.ArgumentMatchers.eq(true),
                encryptedValue.capture(),
                org.mockito.ArgumentMatchers.eq("local-development"));
        assertEquals("new", encryption.decrypt(
                encryptedValue.getValue().ciphertext(),
                encryptedValue.getValue().nonce(),
                encryptedValue.getValue().keyId()));
        assertEquals(2, response.version());
        assertEquals("********", response.value());
    }

    @Test
    void rollbackCopiesPriorValueIntoANewVersion() {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        ConfigEntrySnapshot current = snapshot(
                entryId, environmentId, serviceId, "LOG_LEVEL", false, 3, "warn");
        ConfigEntrySnapshot rolledBack = snapshot(
                entryId, environmentId, serviceId, "LOG_LEVEL", false, 4, "debug");
        ConfigVersionSnapshot historic = versionSnapshot(1, "debug");
        when(assignments.existsByProjectIdAndEnvironmentIdAndServiceId(
                projectId, environmentId, serviceId)).thenReturn(true);
        when(entries.find(environmentId, serviceId, "LOG_LEVEL"))
                .thenReturn(Optional.of(current), Optional.of(rolledBack));
        when(entries.findVersion(entryId, 1)).thenReturn(Optional.of(historic));

        ConfigEntryResponse response = manager.rollback(
                projectId, environmentId, serviceId, "LOG_LEVEL", 1);

        ArgumentCaptor<EncryptedValue> encryptedValue = ArgumentCaptor.forClass(EncryptedValue.class);
        verify(entries).appendRollbackVersion(
                org.mockito.ArgumentMatchers.eq(entryId),
                org.mockito.ArgumentMatchers.eq("LOG_LEVEL"),
                org.mockito.ArgumentMatchers.eq(false),
                encryptedValue.capture(),
                org.mockito.ArgumentMatchers.eq("local-development"),
                org.mockito.ArgumentMatchers.eq(1));
        assertEquals("debug", encryption.decrypt(
                encryptedValue.getValue().ciphertext(),
                encryptedValue.getValue().nonce(),
                encryptedValue.getValue().keyId()));
        assertEquals(4, response.version());
        assertEquals("debug", response.value());
    }

    @Test
    void refusesConfigAccessWhenServiceIsNotAssignedToEnvironment() {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(assignments.existsByProjectIdAndEnvironmentIdAndServiceId(
                projectId, environmentId, serviceId)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> manager.list(projectId, environmentId, serviceId));

        verify(entries, never()).findAll(environmentId, serviceId);
    }

    private ConfigEntrySnapshot snapshot(
            UUID id,
            UUID environmentId,
            UUID serviceId,
            String key,
            boolean secret,
            int version,
            String value) {
        EncryptedValue encrypted = encryption.encrypt(value);
        return new ConfigEntrySnapshot(
                id,
                environmentId,
                serviceId,
                key,
                secret,
                version,
                encrypted.ciphertext(),
                encrypted.nonce(),
                encrypted.keyId(),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private ConfigVersionSnapshot versionSnapshot(int version, String value) {
        EncryptedValue encrypted = encryption.encrypt(value);
        return new ConfigVersionSnapshot(
                UUID.randomUUID(),
                version,
                encrypted.ciphertext(),
                encrypted.nonce(),
                encrypted.keyId(),
                "local-development",
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
