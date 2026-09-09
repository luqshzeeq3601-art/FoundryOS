package com.factoryos.modules.audit.repository;

import com.factoryos.modules.audit.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    @Query("SELECT a FROM AuditEvent a WHERE " +
           "(:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:entityId IS NULL OR a.entityId = :entityId) " +
           "AND (:actorId IS NULL OR a.actorId = :actorId) " +
           "AND (:action IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%', :action, '%'))) " +
           "AND (cast(:fromTime as timestamp) IS NULL OR a.createdAt >= :fromTime) " +
           "AND (cast(:toTime as timestamp) IS NULL OR a.createdAt < :toTime)")
    Page<AuditEvent> searchAuditEvents(
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            @Param("actorId") UUID actorId,
            @Param("action") String action,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            Pageable pageable
    );
}
