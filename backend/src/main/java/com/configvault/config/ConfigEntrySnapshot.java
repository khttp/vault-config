package com.configvault.config;

import java.time.Instant;
import java.util.UUID;

record ConfigEntrySnapshot(
        UUID id,
        UUID environmentId,
        UUID serviceId,
        String key,
        boolean secret,
        int version,
        byte[] ciphertext,
        byte[] nonce,
        String keyId,
        Instant updatedAt) {
}
