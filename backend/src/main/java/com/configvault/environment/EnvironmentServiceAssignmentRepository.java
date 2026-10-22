package com.configvault.environment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentServiceAssignmentRepository
        extends JpaRepository<EnvironmentServiceAssignment, UUID> {

    List<EnvironmentServiceAssignment>
            findAllByProjectIdAndEnvironmentIdOrderByCreatedAtAsc(
                    UUID projectId,
                    UUID environmentId);

    Optional<EnvironmentServiceAssignment>
            findByProjectIdAndEnvironmentIdAndServiceId(
                    UUID projectId,
                    UUID environmentId,
                    UUID serviceId);

    boolean existsByProjectIdAndEnvironmentIdAndServiceId(
            UUID projectId,
            UUID environmentId,
            UUID serviceId);
}
