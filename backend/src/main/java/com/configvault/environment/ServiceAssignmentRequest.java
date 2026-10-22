package com.configvault.environment;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ServiceAssignmentRequest(@NotNull UUID serviceId) {
}
