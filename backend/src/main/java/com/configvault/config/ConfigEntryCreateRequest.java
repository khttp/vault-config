package com.configvault.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConfigEntryCreateRequest(
        @NotBlank
        @Size(max = 255)
        @Pattern(regexp = "[A-Za-z_][A-Za-z0-9_.-]*")
        String key,
        @NotNull @Size(max = 65536) String value,
        boolean secret) {
}
