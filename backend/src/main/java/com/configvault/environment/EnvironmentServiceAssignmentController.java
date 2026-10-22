package com.configvault.environment;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/projects/{projectId}/environments/{environmentId}/services")
@Tag(name = "Environment services", description = "Assign services to project environments")
public class EnvironmentServiceAssignmentController {

    private final EnvironmentServiceAssignmentManager assignments;

    public EnvironmentServiceAssignmentController(
            EnvironmentServiceAssignmentManager assignments) {
        this.assignments = assignments;
    }

    @PostMapping
    @Operation(summary = "Assign a project service to an environment")
    public ResponseEntity<ServiceAssignmentResponse> assign(
            @PathVariable UUID projectId,
            @PathVariable UUID environmentId,
            @Valid @RequestBody ServiceAssignmentRequest request) {
        ServiceAssignmentResponse assignment = assignments.assign(
                projectId, environmentId, request);
        URI location = URI.create(
                "/api/projects/" + projectId + "/environments/" + environmentId
                        + "/services/" + assignment.serviceId());
        return ResponseEntity.created(location).body(assignment);
    }

    @GetMapping
    @Operation(summary = "List services assigned to an environment")
    public List<ServiceAssignmentResponse> list(
            @PathVariable UUID projectId,
            @PathVariable UUID environmentId) {
        return assignments.list(projectId, environmentId);
    }

    @DeleteMapping("/{serviceId}")
    @Operation(summary = "Remove a service from an environment")
    public ResponseEntity<Void> remove(
            @PathVariable UUID projectId,
            @PathVariable UUID environmentId,
            @PathVariable UUID serviceId) {
        assignments.remove(projectId, environmentId, serviceId);
        return ResponseEntity.noContent().build();
    }
}
