
package com.sentinelai.policy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PolicyEvaluationRequest(
        @NotBlank String agentId,
        @NotNull ToolAction action
) {}
