package com.configvault.project;

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

import com.configvault.common.ResourceNotFoundException;
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

@WebMvcTest(ProjectController.class)
@ImportAutoConfiguration({
        SpringDocConfigProperties.class,
        SpringDocConfiguration.class,
        SpringDocWebMvcConfiguration.class,
        MultipleOpenApiSupportConfiguration.class
})
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projects;

    @Test
    void createsProject() throws Exception {
        UUID id = UUID.randomUUID();
        when(projects.create(any(ProjectRequest.class))).thenReturn(
                new ProjectResponse(
                        id,
                        "payments",
                        "Payments service",
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/api/projects")
                        .contentType("application/json")
                        .content("""
                                {"name":"payments","description":"Payments service"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("payments"))
                .andExpect(jsonPath("$.description").value("Payments service"));
    }

    @Test
    void listsGetsUpdatesAndDeletesProjects() throws Exception {
        UUID id = UUID.randomUUID();
        ProjectResponse response = new ProjectResponse(
                id, "commerce", "Commerce app", Instant.parse("2026-01-01T00:00:00Z"));
        when(projects.list()).thenReturn(List.of(response));
        when(projects.get(id)).thenReturn(response);
        when(projects.update(eq(id), any(ProjectRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("commerce"));
        mockMvc.perform(get("/api/projects/{projectId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
        mockMvc.perform(put("/api/projects/{projectId}", id)
                        .contentType("application/json")
                        .content("""
                                {"name":"commerce","description":"Commerce app"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("commerce"));
        mockMvc.perform(delete("/api/projects/{projectId}", id))
                .andExpect(status().isNoContent());
        verify(projects).delete(id);
    }

    @Test
    void rejectsProjectWithBlankName() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType("application/json")
                        .content("""
                                {"name":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("name")));
    }

    @Test
    void returnsNotFoundForUnknownProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projects.get(projectId))
                .thenThrow(new ResourceNotFoundException("Project", projectId));

        mockMvc.perform(get("/api/projects/{projectId}", projectId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Project not found: " + projectId));
    }
}
