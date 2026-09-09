package com.factoryos.modules.audit.dto;

import com.factoryos.modules.audit.domain.AuditEvent;

import java.time.Instant;
import java.util.UUID;

public class AuditDto {
    private UUID id;
    private UUID actorId;
    private String action;
    private String entityType;
    private UUID entityId;
    private String beforeData;
    private String afterData;
    private String traceId;
    private Instant createdAt;

    public AuditDto() {
    }

    public static AuditDto from(AuditEvent event) {
        AuditDto dto = new AuditDto();
        dto.setId(event.getId());
        dto.setActorId(event.getActorId());
        dto.setAction(event.getAction());
        dto.setEntityType(event.getEntityType());
        dto.setEntityId(event.getEntityId());
        dto.setBeforeData(event.getBeforeData());
        dto.setAfterData(event.getAfterData());
        dto.setTraceId(event.getTraceId());
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getBeforeData() {
        return beforeData;
    }

    public void setBeforeData(String beforeData) {
        this.beforeData = beforeData;
    }

    public String getAfterData() {
        return afterData;
    }

    public void setAfterData(String afterData) {
        this.afterData = afterData;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
