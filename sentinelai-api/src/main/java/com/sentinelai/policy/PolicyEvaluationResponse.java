
package com.sentinelai.policy;

public record PolicyEvaluationResponse(
        String agentId,
        String action,
        String decision,
        String reason
) {
}
