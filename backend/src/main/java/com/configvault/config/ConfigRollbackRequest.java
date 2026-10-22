package com.configvault.config;

import jakarta.validation.constraints.Positive;

public record ConfigRollbackRequest(@Positive int version) {
}
