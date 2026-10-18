package com.sentinelai.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "agent_id", length = 120)
    private String agentId;

    @Column(name = "action_name", length = 100)
    private String action;

    @Column(name = "decision", nullable = false, length = 40)
    private String decision;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    protected AuditLog() {
    }

    public AuditLog(
            String agentId,
            String action,
            String decision,
            String reason
    ) {
        this.createdAt = Instant.now();
        this.agentId = agentId;
        this.action = action;
        this.decision = decision;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAction() {
        return action;
    }

    public String getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }
}