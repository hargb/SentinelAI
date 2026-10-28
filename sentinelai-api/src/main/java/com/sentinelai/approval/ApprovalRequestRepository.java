package com.sentinelai.approval;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository
        extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findTop100ByOrderByRequestedAtDesc();

    List<ApprovalRequest> findByStatusOrderByRequestedAtDesc(
            String status);
}