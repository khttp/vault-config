package com.configvault.service;

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

@WebMvcTest(ApplicationServiceController.class)
@ImportAutoConfiguration({
        SpringDocConfigProperties.class,
        SpringDocConfiguration.class,
        SpringDocWebMvcConfiguration.class,
        MultipleOpenApiSupportConfiguration.class
})
class ApplicationServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationServiceManager applicationServices;

    @Test
    void createsServiceForProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(applicationServices.create(eq(projectId), any(ApplicationServiceRequest.class)))
                .thenReturn(new ApplicationServiceResponse(
                        serviceId,
                        projectId,
                        "payments",
                        "Payment processing",
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/api/projects/{projectId}/services", projectId)
                        .contentType("application/json")
                        .content("""
                                {"name":"payments","description":"Payment processing"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(serviceId.toString()))
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("payments"));
    }

    @Test
    void listsGetsUpdatesAndDeletesServices() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        ApplicationServiceResponse response = new ApplicationServiceResponse(
                serviceId,
                projectId,
                "payments",
                "Payment processing",
                Instant.parse("2026-01-01T00:00:00Z"));
        when(applicationServices.list(projectId)).thenReturn(List.of(response));
        when(applicationServices.get(projectId, serviceId)).thenReturn(response);
        when(applicationServices.update(
                eq(projectId), eq(serviceId), any(ApplicationServiceRequest.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/projects/{projectId}/services", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("payments"));
        mockMvc.perform(get("/api/projects/{projectId}/services/{serviceId}", projectId, serviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceId.toString()));
        mockMvc.perform(put(
                        "/api/projects/{projectId}/services/{serviceId}",
                        projectId,
                        serviceId)
                        .contentType("application/json")
                        .content("""
                                {"name":"payments","description":"Payment processing"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("payments"));
        mockMvc.perform(delete(
                        "/api/projects/{projectId}/services/{serviceId}",
                        projectId,
                        serviceId))
                .andExpect(status().isNoContent());
        verify(applicationServices).delete(projectId, serviceId);
    }
}
