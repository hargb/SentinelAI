package com.sentinelai.security;

import com.sentinelai.audit.AuditLogController;
import com.sentinelai.audit.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuditLogSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void normalUserCannotAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                .with(httpBasic("user", "test-password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                .with(httpBasic("admin", "admin-test-password")))
                .andExpect(status().isOk());
    }
}