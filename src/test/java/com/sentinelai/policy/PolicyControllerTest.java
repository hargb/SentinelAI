
package com.sentinelai.policy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void paymentShouldRequireApproval() throws Exception {
        mockMvc.perform(post("/api/v1/policies/evaluate")
                .with(httpBasic("user", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "agentId": "payment-agent",
                      "action": "MAKE_PAYMENT"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision")
                        .value("APPROVAL_REQUIRED"));
    }

    @Test
    void deleteShouldBeDeniedByPolicy() throws Exception {
        mockMvc.perform(post("/api/v1/policies/evaluate")
                .with(httpBasic("user", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "agentId": "reader-agent",
                      "action": "DELETE_RECORD"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DENY"));
    }

    @Test
    void unauthenticatedRequestShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/policies/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "agentId": "payment-agent",
                      "action": "MAKE_PAYMENT"
                    }
                    """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRequestShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/policies/evaluate")
                .with(httpBasic("user", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "agentId": "",
                      "action": "MAKE_PAYMENT"
                    }
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readerCannotMakePayment() throws Exception {
        mockMvc.perform(post("/api/v1/policies/evaluate")
                .with(httpBasic("user", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "agentId": "reader-agent",
                      "action": "MAKE_PAYMENT"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DENY"))
                .andExpect(jsonPath("$.reason")
                        .value("Agent is not authorized to perform this action"));
    }

    @Test
    void unknownAgentShouldBeDenied() throws Exception {
        mockMvc.perform(post("/api/v1/policies/evaluate")
                .with(httpBasic("user", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "agentId": "unknown-agent",
                      "action": "CALCULATE"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DENY"));
    }
}
