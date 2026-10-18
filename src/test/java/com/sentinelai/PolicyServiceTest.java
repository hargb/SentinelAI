package com.sentinelai;

import com.sentinelai.audit.AuditLogService;
import com.sentinelai.policy.PolicyDecision;
import com.sentinelai.policy.PolicyService;
import com.sentinelai.policy.ToolAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PolicyServiceTest {

    private AuditLogService auditLogService;
    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AuditLogService.class);
        policyService = new PolicyService(auditLogService);
    }

    @Test
    void calculatorShouldBeAllowedAndAudited() {
        assertEquals(
                PolicyDecision.ALLOW,
                policyService.evaluate("agent-1", ToolAction.CALCULATE)
        );

        verify(auditLogService).record(
                eq("agent-1"),
                eq("CALCULATE"),
                eq("ALLOW"),
                anyString()
        );
    }

    @Test
    void paymentShouldRequireApprovalAndBeAudited() {
        assertEquals(
                PolicyDecision.APPROVAL_REQUIRED,
                policyService.evaluate("agent-1", ToolAction.MAKE_PAYMENT)
        );

        verify(auditLogService).record(
                eq("agent-1"),
                eq("MAKE_PAYMENT"),
                eq("APPROVAL_REQUIRED"),
                anyString()
        );
    }

    @Test
    void deletingRecordShouldBeDeniedAndAudited() {
        assertEquals(
                PolicyDecision.DENY,
                policyService.evaluate("agent-1", ToolAction.DELETE_RECORD)
        );

        verify(auditLogService).record(
                eq("agent-1"),
                eq("DELETE_RECORD"),
                eq("DENY"),
                anyString()
        );
    }

    @Test
    void missingAgentShouldBeDeniedAndAudited() {
        assertEquals(
                PolicyDecision.DENY,
                policyService.evaluate("", ToolAction.CALCULATE)
        );

        verify(auditLogService).record(
                eq(""),
                eq("CALCULATE"),
                eq("DENY"),
                anyString()
        );
    }
}