package com.configvault;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.configvault.environment.EnvironmentService;
import com.configvault.environment.EnvironmentServiceAssignmentManager;
import com.configvault.config.ConfigEntryManager;
import com.configvault.project.ProjectService;
import com.configvault.service.ApplicationServiceManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;

@WebMvcTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        SpringDocConfigProperties.class,
        SpringDocConfiguration.class,
        SpringDocWebMvcConfiguration.class,
        MultipleOpenApiSupportConfiguration.class
})
class ApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projects;

    @MockitoBean
    private EnvironmentService environments;

    @MockitoBean
    private ApplicationServiceManager applicationServices;

    @MockitoBean
    private EnvironmentServiceAssignmentManager assignments;

    @MockitoBean
    private ConfigEntryManager configs;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private SecurityContextRepository securityContextRepository;

    @Test
    void publishesOpenApiDocumentWithProjectServiceAndEnvironmentEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ConfigVault API"))
                .andExpect(jsonPath("$.paths['/api/projects']").exists())
                .andExpect(jsonPath("$.paths['/api/projects/{projectId}/services']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/projects/{projectId}/environments/{environmentId}/services/{serviceId}/config']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/projects/{projectId}/environments/{environmentId}/services']")
                        .exists());
    }
}
