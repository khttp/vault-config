package com.configvault.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.configvault.project.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceManagerTest {

    @Mock
    private ProjectRepository projects;

    @Mock
    private ApplicationServiceRepository services;

    @InjectMocks
    private ApplicationServiceManager manager;

    @Test
    void returnsExistingServiceWhenRegisteringSameNameAgain() {
        UUID projectId = UUID.randomUUID();
        ApplicationService existing = new ApplicationService(projectId, "payments", "Existing description");
        when(projects.existsById(projectId)).thenReturn(true);
        when(services.findByProjectIdAndName(projectId, "payments")).thenReturn(Optional.of(existing));

        ApplicationServiceResponse response = manager.create(
                projectId,
                new ApplicationServiceRequest(" payments ", "Different description"));

        assertEquals("payments", response.name());
        assertEquals("Existing description", response.description());
        verify(services, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }
}
