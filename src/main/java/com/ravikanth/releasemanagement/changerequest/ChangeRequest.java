
package com.ravikanth.releasemanagement.changerequest;

import com.ravikanth.releasemanagement.environment.Environment;
import com.ravikanth.releasemanagement.release.Release;
import com.ravikanth.releasemanagement.user.User;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "change_requests")
public class ChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id")
    private Release release;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private ChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChangeRequestStatus status = ChangeRequestStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(name = "planned_at")
    private OffsetDateTime plannedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ChangeRequest() {
        // Required by JPA
    }

    public ChangeRequest(
            Environment environment,
            String title,
            String description,
            ChangeType changeType,
            User requestedBy,
            OffsetDateTime plannedAt
    ) {
        this.environment = environment;
        this.title = title;
        this.description = description;
        this.changeType = changeType;
        this.requestedBy = requestedBy;
        this.plannedAt = plannedAt;
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

    public Release getRelease() {
        return release;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public ChangeRequestStatus getStatus() {
        return status;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public OffsetDateTime getPlannedAt() {
        return plannedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(
            String title,
            String description,
            ChangeType changeType,
            OffsetDateTime plannedAt
    ) {
        this.title = title;
        this.description = description;
        this.changeType = changeType;
        this.plannedAt = plannedAt;
    }

    public void submit() {
        if (this.status != ChangeRequestStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only draft change requests can be submitted."
            );
        }

        this.status = ChangeRequestStatus.SUBMITTED;
    }

    public void assignToRelease(Release release) {
        this.release = release;
    }

    public void removeFromRelease() {
        this.release = null;
    }



    public void approve() {
        if (this.status != ChangeRequestStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only submitted change requests can be approved."
            );
        }

        this.status = ChangeRequestStatus.APPROVED;
    }

    public void reject() {
        if (this.status != ChangeRequestStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only submitted change requests can be rejected."
            );
        }

        this.status = ChangeRequestStatus.REJECTED;
    }



    

}
