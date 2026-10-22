package com.configvault.environment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnvironmentRequest(@NotBlank @Size(max = 80) String name) {
}
