
package com.sentinelai.approval;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "approval_requests")
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false, length = 100)
    private String agentId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected ApprovalRequest() {
    }

    public ApprovalRequest(String agentId, String action) {
        this.agentId = agentId;
        this.action = action;
        this.status = "PENDING";
        this.requestedAt = Instant.now();
    }

    public void approve() {
        this.status = "APPROVED";
        this.reviewedAt = Instant.now();
    }

    public void reject() {
        this.status = "REJECTED";
        this.reviewedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAction() {
        return action;
    }

    public String getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
