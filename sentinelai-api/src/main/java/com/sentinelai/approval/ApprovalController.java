
package com.sentinelai.approval;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping
    public List<ApprovalRequest> getRecentRequests() {
        return approvalService.getRecentRequests();
    }

    @PreAuthorize("hasRole('REVIEWER')")
    @PostMapping("/{id}/approve")
    public ApprovalRequest approve(@PathVariable Long id) {
        return approvalService.approve(id);
    }

    @PreAuthorize("hasRole('REVIEWER')")
    @PostMapping("/{id}/reject")
    public ApprovalRequest reject(@PathVariable Long id) {
        return approvalService.reject(id);
    }
}
