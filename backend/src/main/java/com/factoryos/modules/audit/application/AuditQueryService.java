package com.factoryos.modules.audit.application;

import com.factoryos.common.dto.PagedResponse;
import com.factoryos.modules.audit.domain.AuditEvent;
import com.factoryos.modules.audit.dto.AuditDto;
import com.factoryos.modules.audit.repository.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditQueryService {

    private final AuditEventRepository auditEventRepository;

    public AuditQueryService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public PagedResponse<AuditDto> getAuditEvents(
            String entityType,
            UUID entityId,
            UUID actorId,
            String action,
            Instant fromTime,
            Instant toTime,
            Pageable pageable
    ) {
        Page<AuditEvent> page = auditEventRepository.searchAuditEvents(
                entityType, entityId, actorId, action, fromTime, toTime, pageable
        );
        return PagedResponse.from(page.map(AuditDto::from));
    }
}
