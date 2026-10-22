package com.configvault.service;

import java.time.Instant;
import java.util.UUID;

public record ApplicationServiceResponse(
        UUID id,
        UUID projectId,
        String name,
        String description,
        Instant createdAt) {

    public static ApplicationServiceResponse from(ApplicationService service) {
        return new ApplicationServiceResponse(
                service.getId(),
                service.getProjectId(),
                service.getName(),
                service.getDescription(),
                service.getCreatedAt());
    }
}
