package com.sentinelai.policy;

import com.sentinelai.audit.AuditLogService;
import org.springframework.stereotype.Service;

@Service
public class PolicyService {

    private final AuditLogService auditLogService;

    public PolicyService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    public PolicyDecision evaluate(String agentId, ToolAction action) {

        PolicyDecision decision;
        String reason;

        if (agentId == null || agentId.isBlank() || action == null) {
            decision = PolicyDecision.DENY;
            reason = "Missing or invalid agent/action";
        } else {
            decision = switch (action) {
                case CALCULATE, READ_DOCUMENT -> PolicyDecision.ALLOW;
                case SEND_EMAIL, MAKE_PAYMENT -> PolicyDecision.APPROVAL_REQUIRED;
                case DELETE_RECORD -> PolicyDecision.DENY;
            };

            reason = switch (decision) {
                case ALLOW -> "Action permitted by policy";
                case DENY -> "Action denied by policy";
                case APPROVAL_REQUIRED -> "Human approval required";
            };
        }

        auditLogService.record(
                agentId,
                action == null ? null : action.name(),
                decision.name(),
                reason
        );

        return decision;
    }
}