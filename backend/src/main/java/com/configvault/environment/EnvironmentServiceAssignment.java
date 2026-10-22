package com.configvault.environment;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "environment_services",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_environment_services_pair",
                columnNames = {"environment_id", "service_id"}))
public class EnvironmentServiceAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected EnvironmentServiceAssignment() {
    }

    public EnvironmentServiceAssignment(UUID projectId, UUID environmentId, UUID serviceId) {
        this.projectId = projectId;
        this.environmentId = environmentId;
        this.serviceId = serviceId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
