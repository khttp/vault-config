package com.configvault.environment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;

@WebMvcTest(EnvironmentController.class)
@ImportAutoConfiguration({
        SpringDocConfigProperties.class,
        SpringDocConfiguration.class,
        SpringDocWebMvcConfiguration.class,
        MultipleOpenApiSupportConfiguration.class
})
class EnvironmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnvironmentService environments;

    @Test
    void createsEnvironmentForProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        when(environments.create(eq(projectId), any(EnvironmentRequest.class))).thenReturn(
                new EnvironmentResponse(
                        environmentId,
                        projectId,
                        "staging",
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/api/projects/{projectId}/environments", projectId)
                        .contentType("application/json")
                        .content("""
                                {"name":"staging"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(environmentId.toString()))
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("staging"));
    }

    @Test
    void listsGetsUpdatesAndDeletesEnvironments() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        EnvironmentResponse response = new EnvironmentResponse(
                environmentId, projectId, "production", Instant.parse("2026-01-01T00:00:00Z"));
        when(environments.list(projectId)).thenReturn(List.of(response));
        when(environments.get(projectId, environmentId)).thenReturn(response);
        when(environments.update(eq(projectId), eq(environmentId), any(EnvironmentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/projects/{projectId}/environments", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("production"));
        mockMvc.perform(get(
                        "/api/projects/{projectId}/environments/{environmentId}",
                        projectId,
                        environmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(environmentId.toString()));
        mockMvc.perform(put(
                        "/api/projects/{projectId}/environments/{environmentId}",
                        projectId,
                        environmentId)
                        .contentType("application/json")
                        .content("""
                                {"name":"production"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("production"));
        mockMvc.perform(delete(
                        "/api/projects/{projectId}/environments/{environmentId}",
                        projectId,
                        environmentId))
                .andExpect(status().isNoContent());
        verify(environments).delete(projectId, environmentId);
    }
}
