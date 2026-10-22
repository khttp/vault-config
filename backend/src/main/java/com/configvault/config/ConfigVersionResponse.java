package com.configvault.config;

import java.time.Instant;
import java.util.UUID;

public record ConfigVersionResponse(UUID id, int version, String createdBy, Instant createdAt) {

    static ConfigVersionResponse from(ConfigVersionSnapshot version) {
        return new ConfigVersionResponse(
                version.id(),
                version.version(),
                version.createdBy(),
                version.createdAt());
    }
}
