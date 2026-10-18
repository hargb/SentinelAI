
package com.sentinelai.policy;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyService policyService;
    private final AgentAuthorizationService authorizationService;

    public PolicyController(
            PolicyService policyService,
            AgentAuthorizationService authorizationService
    ) {
        this.policyService = policyService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/evaluate")
    public PolicyEvaluationResponse evaluate(
            @Valid @RequestBody PolicyEvaluationRequest request
    ) {
        String agentId = request.agentId();
        ToolAction action = request.action();

        // Step 1: Check whether this agent may perform this action.
        if (!authorizationService.isAuthorized(agentId, action)) {
            return new PolicyEvaluationResponse(
                    agentId,
                    action,
                    PolicyDecision.DENY,
                    "Agent is not authorized to perform this action"
            );
        }

        // Step 2: Apply the existing policy rules.
        PolicyDecision decision = policyService.evaluate(agentId, action);

        String reason = switch (decision) {
            case ALLOW -> "Action allowed by policy";
            case DENY -> "Action denied by policy";
            case APPROVAL_REQUIRED -> "Action requires approval";
        };

        return new PolicyEvaluationResponse(
                agentId,
                action,
                decision,
                reason
        );
    }
}
