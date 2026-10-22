package com.configvault.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationServiceRepository extends JpaRepository<ApplicationService, UUID> {

    List<ApplicationService> findAllByProjectIdOrderByNameAsc(UUID projectId);

    Optional<ApplicationService> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsByIdAndProjectId(UUID id, UUID projectId);
}
