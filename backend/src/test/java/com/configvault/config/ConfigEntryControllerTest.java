package com.configvault.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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

@WebMvcTest(ConfigEntryController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        SpringDocConfigProperties.class,
        SpringDocConfiguration.class,
        SpringDocWebMvcConfiguration.class,
        MultipleOpenApiSupportConfiguration.class
})
class ConfigEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfigEntryManager configs;

    @Test
    void createsConfigEntryAndReturnsMaskedSecret() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        when(configs.create(
                eq(projectId),
                eq(environmentId),
                eq(serviceId),
                any(ConfigEntryCreateRequest.class)))
                .thenReturn(new ConfigEntryResponse(
                        entryId,
                        "PAYMENT_API_KEY",
                        true,
                        "********",
                        1,
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post(configPath(projectId, environmentId, serviceId))
                        .contentType("application/json")
                        .content("""
                                {"key":"PAYMENT_API_KEY","value":"plain-secret","secret":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("PAYMENT_API_KEY"))
                .andExpect(jsonPath("$.secret").value(true))
                .andExpect(jsonPath("$.value").value("********"))
                .andExpect(jsonPath("$.value").value(
                        org.hamcrest.Matchers.not("plain-secret")));
    }

    @Test
    void returnsNonSecretValueAndListsEntries() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        ConfigEntryResponse entry = new ConfigEntryResponse(
                UUID.randomUUID(),
                "LOG_LEVEL",
                false,
                "debug",
                2,
                Instant.parse("2026-01-01T00:00:00Z"));
        when(configs.list(projectId, environmentId, serviceId)).thenReturn(List.of(entry));
        when(configs.get(projectId, environmentId, serviceId, "LOG_LEVEL")).thenReturn(entry);

        mockMvc.perform(get(configPath(projectId, environmentId, serviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("debug"))
                .andExpect(jsonPath("$[0].secret").value(false));
        mockMvc.perform(get(configPath(projectId, environmentId, serviceId) + "/LOG_LEVEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("debug"));
    }

    @Test
    void addsNewVersionForUpdatedConfigEntry() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(configs.update(
                eq(projectId),
                eq(environmentId),
                eq(serviceId),
                eq("LOG_LEVEL"),
                any(ConfigEntryUpdateRequest.class)))
                .thenReturn(new ConfigEntryResponse(
                        UUID.randomUUID(),
                        "LOG_LEVEL",
                        false,
                        "info",
                        3,
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(put(configPath(projectId, environmentId, serviceId) + "/LOG_LEVEL")
                        .contentType("application/json")
                        .content("""
                                {"value":"info"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("info"))
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    void updateDoesNotAllowChangingSecretClassification() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(configs.update(
                eq(projectId),
                eq(environmentId),
                eq(serviceId),
                eq("PAYMENT_API_KEY"),
                any(ConfigEntryUpdateRequest.class)))
                .thenReturn(new ConfigEntryResponse(
                        UUID.randomUUID(),
                        "PAYMENT_API_KEY",
                        true,
                        "********",
                        2,
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(put(
                        configPath(projectId, environmentId, serviceId) + "/PAYMENT_API_KEY")
                        .contentType("application/json")
                        .content("""
                                {"value":"new-secret","secret":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value(true))
                .andExpect(jsonPath("$.value").value("********"));
    }

    @Test
    void listsVersionMetadataWithoutValues() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(configs.history(projectId, environmentId, serviceId, "LOG_LEVEL"))
                .thenReturn(List.of(new ConfigVersionResponse(
                        UUID.randomUUID(),
                        2,
                        "local-development",
                        Instant.parse("2026-01-01T00:00:00Z"))));

        mockMvc.perform(get(
                        configPath(projectId, environmentId, serviceId) + "/LOG_LEVEL/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value(2))
                .andExpect(jsonPath("$[0].value").doesNotExist());
    }

    @Test
    void rollsBackToRequestedVersion() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(configs.rollback(projectId, environmentId, serviceId, "LOG_LEVEL", 1))
                .thenReturn(new ConfigEntryResponse(
                        UUID.randomUUID(),
                        "LOG_LEVEL",
                        false,
                        "debug",
                        4,
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post(
                        configPath(projectId, environmentId, serviceId) + "/LOG_LEVEL/rollback")
                        .contentType("application/json")
                        .content("""
                                {"version":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("debug"))
                .andExpect(jsonPath("$.version").value(4));
    }

    @Test
    void rejectsInvalidConfigKey() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        mockMvc.perform(post(configPath(projectId, environmentId, serviceId))
                        .contentType("application/json")
                        .content("""
                                {"key":"INVALID KEY","value":"value","secret":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("key")));
    }

    private String configPath(UUID projectId, UUID environmentId, UUID serviceId) {
        return "/api/projects/" + projectId
                + "/environments/" + environmentId
                + "/services/" + serviceId
                + "/config";
    }
}
