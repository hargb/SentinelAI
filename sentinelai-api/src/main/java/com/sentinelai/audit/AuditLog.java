
package com.sentinelai.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", length = 100)
    private String agentId;

    @Column(name = "action", length = 100)
    private String action;

    @Column(name = "decision", nullable = false, length = 50)
    private String decision;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(
            String agentId,
            String action,
            String decision,
            String reason) {
        this.agentId = agentId;
        this.action = action;
        this.decision = decision;
        this.reason = reason;
        this.createdAt = Instant.now();
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

    public String getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
