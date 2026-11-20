
package com.ravikanth.releasemanagement.deployment;

import com.ravikanth.releasemanagement.release.Release;
import com.ravikanth.releasemanagement.user.User;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "deployments",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_deployments_release_attempt",
            columnNames = {"release_id", "attempt_number"}
        )
    }
)
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "release_id", nullable = false)
    private Release release;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeploymentStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deployed_by", nullable = false)
    private User deployedBy;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at", nullable = false)
    private OffsetDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    protected Deployment() {
        // Required by JPA
    }

    public Deployment(
            Release release,
            int attemptNumber,
            DeploymentStatus status,
            User deployedBy,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            String notes
    ) {
        this.release = release;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.deployedBy = deployedBy;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public Release getRelease() {
        return release;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public DeploymentStatus getStatus() {
        return status;
    }

    public User getDeployedBy() {
        return deployedBy;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public String getNotes() {
        return notes;
    }
}
