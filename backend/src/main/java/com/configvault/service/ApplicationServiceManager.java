package com.configvault.service;

import java.util.List;
import java.util.UUID;

import com.configvault.common.ResourceNotFoundException;
import com.configvault.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationServiceManager {

    private final ProjectRepository projects;
    private final ApplicationServiceRepository services;

    public ApplicationServiceManager(
            ProjectRepository projects,
            ApplicationServiceRepository services) {
        this.projects = projects;
        this.services = services;
    }

    @Transactional
    public ApplicationServiceResponse create(UUID projectId, ApplicationServiceRequest request) {
        requireProject(projectId);
        ApplicationService service = services.saveAndFlush(new ApplicationService(
                projectId,
                request.name().trim(),
                request.description()));
        return ApplicationServiceResponse.from(service);
    }

    @Transactional(readOnly = true)
    public List<ApplicationServiceResponse> list(UUID projectId) {
        requireProject(projectId);
        return services.findAllByProjectIdOrderByNameAsc(projectId).stream()
                .map(ApplicationServiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationServiceResponse get(UUID projectId, UUID serviceId) {
        return ApplicationServiceResponse.from(findService(projectId, serviceId));
    }

    @Transactional
    public ApplicationServiceResponse update(
            UUID projectId,
            UUID serviceId,
            ApplicationServiceRequest request) {
        ApplicationService service = findService(projectId, serviceId);
        service.update(request.name().trim(), request.description());
        return ApplicationServiceResponse.from(services.saveAndFlush(service));
    }

    @Transactional
    public void delete(UUID projectId, UUID serviceId) {
        services.delete(findService(projectId, serviceId));
    }

    private void requireProject(UUID projectId) {
        if (!projects.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", projectId);
        }
    }

    private ApplicationService findService(UUID projectId, UUID serviceId) {
        return services.findByIdAndProjectId(serviceId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));
    }
}
