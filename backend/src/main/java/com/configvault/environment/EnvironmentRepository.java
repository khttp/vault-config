package com.configvault.environment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentRepository extends JpaRepository<Environment, UUID> {

    List<Environment> findAllByProjectIdOrderByNameAsc(UUID projectId);

    Optional<Environment> findByIdAndProjectId(UUID id, UUID projectId);
}
