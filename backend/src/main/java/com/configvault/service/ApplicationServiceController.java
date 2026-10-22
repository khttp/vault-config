package com.configvault.service;

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
@RequestMapping("/api/projects/{projectId}/services")
@Tag(name = "Services", description = "Manage services that belong to a project")
public class ApplicationServiceController {

    private final ApplicationServiceManager services;

    public ApplicationServiceController(ApplicationServiceManager services) {
        this.services = services;
    }

    @PostMapping
    @Operation(summary = "Create a service in a project")
    public ResponseEntity<ApplicationServiceResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody ApplicationServiceRequest request) {
        ApplicationServiceResponse created = services.create(projectId, request);
        return ResponseEntity.created(URI.create(
                "/api/projects/" + projectId + "/services/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List a project's services")
    public List<ApplicationServiceResponse> list(@PathVariable UUID projectId) {
        return services.list(projectId);
    }

    @GetMapping("/{serviceId}")
    @Operation(summary = "Get a project's service")
    public ApplicationServiceResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID serviceId) {
        return services.get(projectId, serviceId);
    }

    @PutMapping("/{serviceId}")
    @Operation(summary = "Update a project's service")
    public ApplicationServiceResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID serviceId,
            @Valid @RequestBody ApplicationServiceRequest request) {
        return services.update(projectId, serviceId, request);
    }

    @DeleteMapping("/{serviceId}")
    @Operation(summary = "Delete a project's service")
    public ResponseEntity<Void> delete(
            @PathVariable UUID projectId,
            @PathVariable UUID serviceId) {
        services.delete(projectId, serviceId);
        return ResponseEntity.noContent().build();
    }
}
