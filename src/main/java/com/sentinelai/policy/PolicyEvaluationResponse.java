
package com.sentinelai.policy;

public record PolicyEvaluationResponse(
        String agentId,
        ToolAction action,
        PolicyDecision decision,
        String reason
) {}
