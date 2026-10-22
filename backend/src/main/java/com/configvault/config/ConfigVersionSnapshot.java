package com.configvault.config;

import java.time.Instant;
import java.util.UUID;

record ConfigVersionSnapshot(
        UUID id,
        int version,
        byte[] ciphertext,
        byte[] nonce,
        String keyId,
        String createdBy,
        Instant createdAt) {
}
