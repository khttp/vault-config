package com.configvault.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConfigEntryUpdateRequest(
        @NotNull @Size(max = 65536) String value) {
}
