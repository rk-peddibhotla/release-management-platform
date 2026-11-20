
package com.ravikanth.releasemanagement.approval;

import com.ravikanth.releasemanagement.changerequest.ChangeRequest;
import com.ravikanth.releasemanagement.user.User;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "approvals")
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "change_request_id", nullable = false, unique = true)
    private ChangeRequest changeRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalDecision decision;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private OffsetDateTime decidedAt;

    protected Approval() {
        // Required by JPA
    }

    public Approval(
            ChangeRequest changeRequest,
            User approver,
            ApprovalDecision decision,
            String comments
    ) {
        this.changeRequest = changeRequest;
        this.approver = approver;
        this.decision = decision;
        this.comments = comments;
        this.decidedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ChangeRequest getChangeRequest() {
        return changeRequest;
    }

    public User getApprover() {
        return approver;
    }

    public ApprovalDecision getDecision() {
        return decision;
    }

    public String getComments() {
        return comments;
    }

    public OffsetDateTime getDecidedAt() {
        return decidedAt;
    }
}
