
package com.sentinelai.audit;

import com.sentinelai.policy.PolicyDecision;
import com.sentinelai.policy.PolicyService;
import com.sentinelai.policy.ToolAction;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("dev")
@Transactional
class AuditLogIntegrationTest {

    private static final String TEST_AGENT_ID =
            "integration-test-agent-2026";

    @Autowired
    private PolicyService policyService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void policyDecisionsShouldBePersisted() {

        assertEquals(
                PolicyDecision.ALLOW,
                policyService.evaluate(
                        TEST_AGENT_ID,
                        ToolAction.CALCULATE
                )
        );

        assertEquals(
                PolicyDecision.APPROVAL_REQUIRED,
                policyService.evaluate(
                        TEST_AGENT_ID,
                        ToolAction.MAKE_PAYMENT
                )
        );

        assertEquals(
                PolicyDecision.DENY,
                policyService.evaluate(
                        TEST_AGENT_ID,
                        ToolAction.DELETE_RECORD
                )
        );

        List<AuditLog> logs = auditLogRepository.findAll().stream()
                .filter(log -> TEST_AGENT_ID.equals(log.getAgentId()))
                .toList();

        assertEquals(3, logs.size());

        assertTrue(logs.stream().anyMatch(log ->
                "CALCULATE".equals(log.getAction())
                        && "ALLOW".equals(log.getDecision())
        ));

        assertTrue(logs.stream().anyMatch(log ->
                "MAKE_PAYMENT".equals(log.getAction())
                        && "APPROVAL_REQUIRED".equals(log.getDecision())
        ));

        assertTrue(logs.stream().anyMatch(log ->
                "DELETE_RECORD".equals(log.getAction())
                        && "DENY".equals(log.getDecision())
        ));

        assertTrue(logs.stream().allMatch(log ->
                log.getCreatedAt() != null
                        && log.getReason() != null
        ));
    }
}
