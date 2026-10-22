package com.configvault.environment;

import java.util.List;
import java.util.UUID;

import com.configvault.common.ResourceNotFoundException;
import com.configvault.service.ApplicationServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentServiceAssignmentManager {

    private final EnvironmentRepository environments;
    private final ApplicationServiceRepository services;
    private final EnvironmentServiceAssignmentRepository assignments;

    public EnvironmentServiceAssignmentManager(
            EnvironmentRepository environments,
            ApplicationServiceRepository services,
            EnvironmentServiceAssignmentRepository assignments) {
        this.environments = environments;
        this.services = services;
        this.assignments = assignments;
    }

    @Transactional
    public ServiceAssignmentResponse assign(
            UUID projectId,
            UUID environmentId,
            ServiceAssignmentRequest request) {
        requireEnvironment(projectId, environmentId);
        if (!services.existsByIdAndProjectId(request.serviceId(), projectId)) {
            throw new ResourceNotFoundException("Service", request.serviceId());
        }

        EnvironmentServiceAssignment assignment = assignments.saveAndFlush(
                new EnvironmentServiceAssignment(projectId, environmentId, request.serviceId()));
        return ServiceAssignmentResponse.from(assignment);
    }

    @Transactional(readOnly = true)
    public List<ServiceAssignmentResponse> list(UUID projectId, UUID environmentId) {
        requireEnvironment(projectId, environmentId);
        return assignments
                .findAllByProjectIdAndEnvironmentIdOrderByCreatedAtAsc(projectId, environmentId)
                .stream()
                .map(ServiceAssignmentResponse::from)
                .toList();
    }

    @Transactional
    public void remove(UUID projectId, UUID environmentId, UUID serviceId) {
        EnvironmentServiceAssignment assignment = assignments
                .findByProjectIdAndEnvironmentIdAndServiceId(
                        projectId, environmentId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service assignment", environmentId + "/" + serviceId));
        assignments.delete(assignment);
    }

    private void requireEnvironment(UUID projectId, UUID environmentId) {
        if (environments.findByIdAndProjectId(environmentId, projectId).isEmpty()) {
            throw new ResourceNotFoundException("Environment", environmentId);
        }
    }
}
