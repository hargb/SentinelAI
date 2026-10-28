
package com.sentinelai.policy;

import com.sentinelai.approval.ApprovalService;
import com.sentinelai.audit.AuditLogService;
import org.springframework.stereotype.Service;

@Service
public class PolicyService {

    private final AgentAuthorizationService authorizationService;
    private final AuditLogService auditLogService;
    private final ApprovalService approvalService;

    public PolicyService(
            AgentAuthorizationService authorizationService,
            AuditLogService auditLogService,
            ApprovalService approvalService) {
        this.authorizationService = authorizationService;
        this.auditLogService = auditLogService;
        this.approvalService = approvalService;
    }

    public PolicyEvaluationResponse evaluate(
            PolicyEvaluationRequest request) {

        if (request == null) {
            return recordDecision(
                    null, null, "DENY",
                    "Request body is required");
        }

        String agentId = request.agentId();
        String action = request.action();

        if (agentId == null || agentId.isBlank()
                || action == null || action.isBlank()) {
            return recordDecision(
                    agentId, action, "DENY",
                    "Agent ID and action are required");
        }

        agentId = agentId.trim();
        action = action.trim().toUpperCase();

        if (!authorizationService.isKnownAgent(agentId)) {
            return recordDecision(
                    agentId, action, "DENY", "Unknown agent");
        }

        if (!authorizationService.isActionAllowed(agentId, action)) {
            return recordDecision(
                    agentId, action, "DENY",
                    "Agent is not authorized for this action");
        }

        if ("MAKE_PAYMENT".equals(action)) {
            approvalService.createRequest(agentId, action);

            return recordDecision(
                    agentId, action, "APPROVAL_REQUIRED",
                    "Payment requires human approval");
        }

        return recordDecision(
                agentId, action, "ALLOW",
                "Action is allowed by agent permissions");
    }

    private PolicyEvaluationResponse recordDecision(
            String agentId,
            String action,
            String decision,
            String reason) {

        PolicyEvaluationResponse response =
                new PolicyEvaluationResponse(
                        agentId, action, decision, reason);

        auditLogService.record(
                agentId, action, decision, reason);

        return response;
    }
}
