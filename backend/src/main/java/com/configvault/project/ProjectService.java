package com.configvault.project;

import java.util.List;
import java.util.UUID;

import com.configvault.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projects;

    public ProjectService(ProjectRepository projects) {
        this.projects = projects;
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        Project project = projects.saveAndFlush(
                new Project(request.name().trim(), request.description()));
        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projects.findAllByOrderByNameAsc().stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID id) {
        return ProjectResponse.from(findProject(id));
    }

    @Transactional
    public ProjectResponse update(UUID id, ProjectRequest request) {
        Project project = findProject(id);
        project.update(request.name().trim(), request.description());
        return ProjectResponse.from(projects.saveAndFlush(project));
    }

    @Transactional
    public void delete(UUID id) {
        projects.delete(findProject(id));
    }

    private Project findProject(UUID id) {
        return projects.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }
}
