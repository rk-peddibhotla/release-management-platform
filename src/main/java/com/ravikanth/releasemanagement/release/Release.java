
package com.ravikanth.releasemanagement.release;

import com.ravikanth.releasemanagement.environment.Environment;
import com.ravikanth.releasemanagement.user.User;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "releases",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_releases_environment_version",
            columnNames = {"environment_id", "version"}
        ),
        @UniqueConstraint(
            name = "uk_releases_id_environment",
            columnNames = {"id", "environment_id"}
        )
    }
)
public class Release {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReleaseStatus status = ReleaseStatus.SCHEDULED;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Release() {
        // Required by JPA
    }

    public Release(
            Environment environment,
            String version,
            String description,
            OffsetDateTime scheduledAt,
            User createdBy
    ) {
        this.environment = environment;
        this.version = version;
        this.description = description;
        this.scheduledAt = scheduledAt;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public ReleaseStatus getStatus() {
        return status;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(
            String version,
            String description,
            OffsetDateTime scheduledAt
    ) {
        this.version = version;
        this.description = description;
        this.scheduledAt = scheduledAt;
    }

    public void start() {
        if (this.status != ReleaseStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Only scheduled releases can be started."
            );
        }

        this.status = ReleaseStatus.IN_PROGRESS;
    }

    public void complete() {
        if (this.status != ReleaseStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Only in-progress releases can be completed."
            );
        }

        this.status = ReleaseStatus.COMPLETED;
    }

    public void cancel() {
        if (this.status == ReleaseStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Completed releases cannot be cancelled."
            );
        }

        if (this.status == ReleaseStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Release is already cancelled."
            );
        }

        this.status = ReleaseStatus.CANCELLED;
    }
}
