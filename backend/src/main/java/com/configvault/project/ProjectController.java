package com.configvault.project;

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
@RequestMapping("/api/projects")
@Tag(name = "Projects", description = "Create and manage ConfigVault projects")
public class ProjectController {

    private final ProjectService projects;

    public ProjectController(ProjectService projects) {
        this.projects = projects;
    }

    @PostMapping
    @Operation(summary = "Create a project")
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        ProjectResponse created = projects.create(request);
        return ResponseEntity.created(URI.create("/api/projects/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List projects")
    public List<ProjectResponse> list() {
        return projects.list();
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get a project")
    public ProjectResponse get(@PathVariable UUID projectId) {
        return projects.get(projectId);
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Update a project")
    public ProjectResponse update(
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectRequest request) {
        return projects.update(projectId, request);
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete a project")
    public ResponseEntity<Void> delete(@PathVariable UUID projectId) {
        projects.delete(projectId);
        return ResponseEntity.noContent().build();
    }
}
