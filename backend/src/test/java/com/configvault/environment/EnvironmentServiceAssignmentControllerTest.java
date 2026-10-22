package com.configvault.environment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;

@WebMvcTest(EnvironmentServiceAssignmentController.class)
@ImportAutoConfiguration({
        SpringDocConfigProperties.class,
        SpringDocConfiguration.class,
        SpringDocWebMvcConfiguration.class,
        MultipleOpenApiSupportConfiguration.class
})
class EnvironmentServiceAssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnvironmentServiceAssignmentManager assignments;

    @Test
    void assignsServiceToEnvironment() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(assignments.assign(
                eq(projectId),
                eq(environmentId),
                any(ServiceAssignmentRequest.class)))
                .thenReturn(new ServiceAssignmentResponse(
                        UUID.randomUUID(),
                        projectId,
                        environmentId,
                        serviceId,
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post(
                        "/api/projects/{projectId}/environments/{environmentId}/services",
                        projectId,
                        environmentId)
                        .contentType("application/json")
                        .content("""
                                {"serviceId":"%s"}
                                """.formatted(serviceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.environmentId").value(environmentId.toString()))
                .andExpect(jsonPath("$.serviceId").value(serviceId.toString()));
    }

    @Test
    void returnsConflictWhenServiceIsAlreadyAssigned() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(assignments.assign(
                eq(projectId),
                eq(environmentId),
                any(ServiceAssignmentRequest.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate assignment"));

        mockMvc.perform(post(
                        "/api/projects/{projectId}/environments/{environmentId}/services",
                        projectId,
                        environmentId)
                        .contentType("application/json")
                        .content("""
                                {"serviceId":"%s"}
                                """.formatted(serviceId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("A resource with those values already exists"));
    }

    @Test
    void listsAndRemovesServicesAssignedToEnvironment() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        ServiceAssignmentResponse response = new ServiceAssignmentResponse(
                UUID.randomUUID(),
                projectId,
                environmentId,
                serviceId,
                Instant.parse("2026-01-01T00:00:00Z"));
        when(assignments.list(projectId, environmentId)).thenReturn(List.of(response));

        mockMvc.perform(get(
                        "/api/projects/{projectId}/environments/{environmentId}/services",
                        projectId,
                        environmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceId").value(serviceId.toString()));
        mockMvc.perform(delete(
                        "/api/projects/{projectId}/environments/{environmentId}/services/{serviceId}",
                        projectId,
                        environmentId,
                        serviceId))
                .andExpect(status().isNoContent());
        verify(assignments).remove(projectId, environmentId, serviceId);
    }
}
