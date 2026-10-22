package com.configvault.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.configvault.common.ResourceNotFoundException;
import com.configvault.service.ApplicationServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceAssignmentManagerTest {

    @Mock
    private EnvironmentRepository environments;

    @Mock
    private ApplicationServiceRepository services;

    @Mock
    private EnvironmentServiceAssignmentRepository assignments;

    private EnvironmentServiceAssignmentManager manager;

    @BeforeEach
    void setUp() {
        manager = new EnvironmentServiceAssignmentManager(environments, services, assignments);
    }

    @Test
    void assignsServiceWhenBothBelongToTheProject() {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(environments.findByIdAndProjectId(environmentId, projectId))
                .thenReturn(Optional.of(new Environment(projectId, "staging")));
        when(services.existsByIdAndProjectId(serviceId, projectId)).thenReturn(true);
        when(assignments.saveAndFlush(any(EnvironmentServiceAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        manager.assign(projectId, environmentId, new ServiceAssignmentRequest(serviceId));

        ArgumentCaptor<EnvironmentServiceAssignment> assignment =
                ArgumentCaptor.forClass(EnvironmentServiceAssignment.class);
        verify(assignments).saveAndFlush(assignment.capture());
        assertEquals(projectId, assignment.getValue().getProjectId());
        assertEquals(environmentId, assignment.getValue().getEnvironmentId());
        assertEquals(serviceId, assignment.getValue().getServiceId());
    }

    @Test
    void rejectsEnvironmentFromAnotherProject() {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(environments.findByIdAndProjectId(environmentId, projectId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> manager.assign(
                projectId,
                environmentId,
                new ServiceAssignmentRequest(serviceId)));

        verify(services, never()).existsByIdAndProjectId(serviceId, projectId);
        verify(assignments, never()).saveAndFlush(any(EnvironmentServiceAssignment.class));
    }

    @Test
    void rejectsServiceFromAnotherProject() {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(environments.findByIdAndProjectId(environmentId, projectId))
                .thenReturn(Optional.of(new Environment(projectId, "staging")));
        when(services.existsByIdAndProjectId(serviceId, projectId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> manager.assign(
                projectId,
                environmentId,
                new ServiceAssignmentRequest(serviceId)));

        verify(assignments, never()).saveAndFlush(any(EnvironmentServiceAssignment.class));
    }
}
