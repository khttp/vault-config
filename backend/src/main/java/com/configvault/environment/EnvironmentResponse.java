package com.configvault.environment;

import java.time.Instant;
import java.util.UUID;

public record EnvironmentResponse(UUID id, UUID projectId, String name, Instant createdAt) {

    public static EnvironmentResponse from(Environment environment) {
        return new EnvironmentResponse(
                environment.getId(),
                environment.getProjectId(),
                environment.getName(),
                environment.getCreatedAt());
    }
}
