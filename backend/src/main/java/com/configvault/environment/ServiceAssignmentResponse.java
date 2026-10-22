package com.configvault.environment;

import java.time.Instant;
import java.util.UUID;

public record ServiceAssignmentResponse(
        UUID id,
        UUID projectId,
        UUID environmentId,
        UUID serviceId,
        Instant assignedAt) {

    public static ServiceAssignmentResponse from(EnvironmentServiceAssignment assignment) {
        return new ServiceAssignmentResponse(
                assignment.getId(),
                assignment.getProjectId(),
                assignment.getEnvironmentId(),
                assignment.getServiceId(),
                assignment.getCreatedAt());
    }
}
