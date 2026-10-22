package com.configvault.environment;

import java.util.List;
import java.util.UUID;

import com.configvault.common.ResourceNotFoundException;
import com.configvault.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentService {

    private final ProjectRepository projects;
    private final EnvironmentRepository environments;

    public EnvironmentService(ProjectRepository projects, EnvironmentRepository environments) {
        this.projects = projects;
        this.environments = environments;
    }

    @Transactional
    public EnvironmentResponse create(UUID projectId, EnvironmentRequest request) {
        requireProject(projectId);
        Environment environment = environments.saveAndFlush(
                new Environment(projectId, request.name().trim()));
        return EnvironmentResponse.from(environment);
    }

    @Transactional(readOnly = true)
    public List<EnvironmentResponse> list(UUID projectId) {
        requireProject(projectId);
        return environments.findAllByProjectIdOrderByNameAsc(projectId).stream()
                .map(EnvironmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnvironmentResponse get(UUID projectId, UUID environmentId) {
        return EnvironmentResponse.from(findEnvironment(projectId, environmentId));
    }

    @Transactional
    public EnvironmentResponse update(
            UUID projectId,
            UUID environmentId,
            EnvironmentRequest request) {
        Environment environment = findEnvironment(projectId, environmentId);
        environment.updateName(request.name().trim());
        return EnvironmentResponse.from(environments.saveAndFlush(environment));
    }

    @Transactional
    public void delete(UUID projectId, UUID environmentId) {
        environments.delete(findEnvironment(projectId, environmentId));
    }

    private void requireProject(UUID projectId) {
        if (!projects.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", projectId);
        }
    }

    private Environment findEnvironment(UUID projectId, UUID environmentId) {
        return environments.findByIdAndProjectId(environmentId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Environment", environmentId));
    }
}
