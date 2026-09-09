package com.factoryos.modules.audit.application;

import com.factoryos.modules.audit.domain.AuditEvent;
import com.factoryos.modules.audit.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditRecordingService {

    private static final Logger log = LoggerFactory.getLogger(AuditRecordingService.class);
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public AuditRecordingService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Mandatory Invariant: Consequential business mutations and their audit entries commit atomically
     * in the SAME transaction (Propagation.MANDATORY or REQUIRED). If audit fails, mutation rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(UUID actorId, String action, String entityType, UUID entityId, Map<String, Object> before, Map<String, Object> after) {
        try {
            AuditEvent event = new AuditEvent();
            event.setActorId(actorId);
            event.setAction(action);
            event.setEntityType(entityType);
            event.setEntityId(entityId);
            event.setCreatedBy(actorId);

            String traceId = MDC.get("traceId");
            event.setTraceId(traceId != null ? traceId : UUID.randomUUID().toString());

            if (before != null && !before.isEmpty()) {
                event.setBeforeData(objectMapper.writeValueAsString(before));
            }
            if (after != null && !after.isEmpty()) {
                event.setAfterData(objectMapper.writeValueAsString(after));
            }

            auditEventRepository.save(event);
            log.info("Audit recorded: action={}, entityType={}, entityId={}, actorId={}", action, entityType, entityId, actorId);
        } catch (Exception e) {
            log.error("Failed to persist audit event for action={}, entityId={}", action, entityId, e);
            throw new RuntimeException("Audit event persistence failure triggered transaction rollback", e);
        }
    }
}
