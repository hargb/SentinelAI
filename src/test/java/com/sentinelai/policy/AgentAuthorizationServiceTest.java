
package com.sentinelai.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentAuthorizationServiceTest {

    private final AgentAuthorizationService service =
            new AgentAuthorizationService();

    @Test
    void readerCanReadDocuments() {
        assertTrue(service.isAuthorized(
                "reader-agent",
                ToolAction.READ_DOCUMENT
        ));
    }

    @Test
    void readerCannotMakePayments() {
        assertFalse(service.isAuthorized(
                "reader-agent",
                ToolAction.MAKE_PAYMENT
        ));
    }

    @Test
    void unknownAgentIsDenied() {
        assertFalse(service.isAuthorized(
                "unknown-agent",
                ToolAction.CALCULATE
        ));
    }

    @Test
    void blankAgentIsDenied() {
        assertFalse(service.isAuthorized(
                " ",
                ToolAction.READ_DOCUMENT
        ));
    }
}
