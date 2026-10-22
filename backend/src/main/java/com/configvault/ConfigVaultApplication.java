package com.configvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "ConfigVault API",
        version = "v1",
        description = "Local development API for managing ConfigVault projects and environments"))
public class ConfigVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigVaultApplication.class, args);
    }
}
