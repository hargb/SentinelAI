
package com.sentinelai.policy;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class AgentAuthorizationService {

    private final Map<String, Set<AgentPermission>> permissions =
            Map.of(
                    "reader-agent", Set.of(
                            AgentPermission.READ_DOCUMENT
                    ),
                    "analyst-agent", Set.of(
                            AgentPermission.CALCULATE,
                            AgentPermission.READ_DOCUMENT
                    ),
                    "payment-agent", Set.of(
                            AgentPermission.MAKE_PAYMENT,
                            AgentPermission.READ_DOCUMENT
                    )
            );

    public boolean isAuthorized(
            String agentId,
            ToolAction action
    ) {
        if (agentId == null || agentId.isBlank() || action == null) {
            return false;
        }

        Set<AgentPermission> agentPermissions =
                permissions.get(agentId);

        if (agentPermissions == null) {
            return false;
        }

        try {
            AgentPermission requiredPermission =
                    AgentPermission.valueOf(action.name());

            return agentPermissions.contains(requiredPermission);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
