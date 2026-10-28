
package com.sentinelai.policy;

public record PolicyEvaluationRequest(
        String agentId,
        String action
) {
}
