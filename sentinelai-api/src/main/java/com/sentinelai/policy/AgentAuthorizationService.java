
package com.sentinelai.policy;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class AgentAuthorizationService {

    private final Map<String, Set<String>> permissions = Map.of(
        "reader-agent", Set.of("READ_DOCUMENT"),
        "analyst-agent", Set.of("READ_DOCUMENT", "CALCULATE"),
        "payment-agent", Set.of("READ_DOCUMENT", "CALCULATE", "MAKE_PAYMENT")
    );

    public boolean isKnownAgent(String agentId) {
        return agentId != null && permissions.containsKey(agentId);
    }

    public boolean isActionAllowed(String agentId, String action) {
        if (agentId == null || action == null) {
            return false;
        }

        Set<String> allowedActions = permissions.get(agentId);

        return allowedActions != null && allowedActions.contains(action);
    }
}
