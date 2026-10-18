package com.sentinelai.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(
            String agentId,
            String action,
            String decision,
            String reason
    ) {
        AuditLog log = new AuditLog(
                agentId,
                action,
                decision,
                reason
        );

        repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findRecent() {
        return repository.findTop100ByOrderByCreatedAtDesc();
    }
}