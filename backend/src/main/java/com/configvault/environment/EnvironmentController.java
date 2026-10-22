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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/projects/{projectId}/environments")
@Tag(name = "Environments", description = "Manage project environments")
public class EnvironmentController {

    private final EnvironmentService environments;

    public EnvironmentController(EnvironmentService environments) {
        this.environments = environments;
    }

    @PostMapping
    @Operation(summary = "Create an environment for a project")
    public ResponseEntity<EnvironmentResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody EnvironmentRequest request) {
        EnvironmentResponse created = environments.create(projectId, request);
        URI location = URI.create(
                "/api/projects/" + projectId + "/environments/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List a project's environments")
    public List<EnvironmentResponse> list(@PathVariable UUID projectId) {
        return environments.list(projectId);
    }

    @GetMapping("/{environmentId}")
    @Operation(summary = "Get a project's environment")
    public EnvironmentResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID environmentId) {
        return environments.get(projectId, environmentId);
    }

    @PutMapping("/{environmentId}")
    @Operation(summary = "Update a project's environment")
    public EnvironmentResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID environmentId,
            @Valid @RequestBody EnvironmentRequest request) {
        return environments.update(projectId, environmentId, request);
    }

    @DeleteMapping("/{environmentId}")
    @Operation(summary = "Delete a project's environment")
    public ResponseEntity<Void> delete(
            @PathVariable UUID projectId,
            @PathVariable UUID environmentId) {
        environments.delete(projectId, environmentId);
        return ResponseEntity.noContent().build();
    }
}
