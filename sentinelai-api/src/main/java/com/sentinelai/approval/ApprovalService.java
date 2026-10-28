package com.sentinelai.approval;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApprovalService {

    private final ApprovalRequestRepository repository;

    public ApprovalService(ApprovalRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ApprovalRequest createRequest(String agentId, String action) {
        ApprovalRequest request = new ApprovalRequest(agentId, action);
        return repository.save(request);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getRecentRequests() {
        return repository.findTop100ByOrderByRequestedAtDesc();
    }

    @Transactional
    public ApprovalRequest approve(Long id) {
        ApprovalRequest request = getRequestOrThrow(id);

        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending requests can be approved");
        }

        request.approve();
        return repository.save(request);
    }

    @Transactional
    public ApprovalRequest reject(Long id) {
        ApprovalRequest request = getRequestOrThrow(id);

        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending requests can be rejected");
        }

        request.reject();
        return repository.save(request);
    }

    private ApprovalRequest getRequestOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Approval request not found"));
    }
}