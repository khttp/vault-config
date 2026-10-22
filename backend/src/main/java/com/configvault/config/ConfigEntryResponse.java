package com.configvault.config;

import java.time.Instant;
import java.util.UUID;

public record ConfigEntryResponse(
        UUID id,
        String key,
        boolean secret,
        String value,
        int version,
        Instant updatedAt) {

    static ConfigEntryResponse from(ConfigEntrySnapshot entry, String value) {
        return new ConfigEntryResponse(
                entry.id(),
                entry.key(),
                entry.secret(),
                value,
                entry.version(),
                entry.updatedAt());
    }
}
