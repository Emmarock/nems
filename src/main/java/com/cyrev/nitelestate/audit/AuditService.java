package com.cyrev.nitelestate.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /** Runs in its own transaction so an audit-write failure never rolls back the business change it describes. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String entityType, Object entityId, String action, String details) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(String.valueOf(entityId));
        log.setAction(action);
        log.setActor(currentActor());
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }
}
