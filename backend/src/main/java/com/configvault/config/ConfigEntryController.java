package com.configvault.config;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
@RequestMapping(
        "/api/projects/{projectId}/environments/{environmentId}/services/{serviceId}/config")
@Tag(name = "Configuration", description = "Manage versioned configuration for a service")
public class ConfigEntryController {

    private final ConfigEntryManager configs;

    public ConfigEntryController(ConfigEntryManager configs) {
        this.configs = configs;
    }

    @PostMapping
    @Operation(summary = "Create a configuration entry")
    public ResponseEntity<ConfigEntryResponse> create(
            @PathVariable UUID projectId,
            @PathVariable UUID environmentId,
            @PathVariable UUID serviceId,
            @Valid @RequestBody ConfigEntryCreateRequest request) {
        ConfigEntryResponse created = configs.create(
                projectId, environmentId, serviceId, request);
        return ResponseEntity.created(URI.create(
                "/api/projects/" + projectId + "/environments/" + environmentId
                        + "/services/" + serviceId + "/config/" + created.key()))
                .body(created);
    }

    @GetMapping
    @Operation(summary = "List configuration entries; secret values are masked")
    public List<ConfigEntryResponse> list(
            @PathVariable UUID projectId,
            @PathVariable UUID environmentId,
            @PathVariable UUID serviceId) {
        return configs.list(projectId, environmentId, serviceId);
    }

    @GetMapping("/{key}")
    @Operation(summary = "Get a configuration entry; secret values are masked")
    public ConfigEntryResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID environmentId,
            @PathVariable UUID serviceId,
            @PathVariable String key) {
        return configs.get(projectId, environmentId, serviceId, key);
    }

    @PutMapping("/{key}")
    @Operation(summary = "Create a new version of a configuration entry")
    public ConfigEntryResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID environmentId,
            @PathVariable UUID serviceId,
            @PathVariable String key,
            @Valid @RequestBody ConfigEntryUpdateRequest request) {
        return configs.update(projectId, environmentId, serviceId, key, request);
    }

    @GetMapping("/{key}/versions")
    @Operation(summary = "List configuration version metadata")
    public List<ConfigVersionResponse> history(
            @PathVariable UUID projectId,
            @PathVariable UUID environmentId,
            @PathVariable UUID serviceId,
            @PathVariable String key) {
        return configs.history(projectId, environmentId, serviceId, key);
    }

    @PostMapping("/{key}/rollback")
    @Operation(summary = "Restore a previous value as a new version")
    public ConfigEntryResponse rollback(
            @PathVariable UUID projectId,
            @PathVariable UUID environmentId,
            @PathVariable UUID serviceId,
            @PathVariable String key,
            @Valid @RequestBody ConfigRollbackRequest request) {
        return configs.rollback(
                projectId, environmentId, serviceId, key, request.version());
    }
}
