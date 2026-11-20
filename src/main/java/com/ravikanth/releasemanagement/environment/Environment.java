
package com.ravikanth.releasemanagement.environment;

import com.ravikanth.releasemanagement.application.Application;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "environments",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_environments_application_name",
            columnNames = {"application_id", "name"}
        )
    }
)
public class Environment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type", nullable = false, length = 20)
    private EnvironmentType environmentType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Environment() {
        // Required by JPA
    }

    public Environment(
            Application application,
            String name,
            EnvironmentType environmentType
    ) {
        this.application = application;
        this.name = name;
        this.environmentType = environmentType;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public String getName() {
        return name;
    }

    public EnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void updateDetails(
            String name,
            EnvironmentType environmentType
    ) {
        this.name = name;
        this.environmentType = environmentType;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
