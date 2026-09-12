package com.factoryos.modules.downtime.repository;

import com.factoryos.modules.downtime.domain.DowntimeEvent;
import com.factoryos.modules.machine.domain.Machine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DowntimeEventRepository extends JpaRepository<DowntimeEvent, UUID> {

    Optional<DowntimeEvent> findByMachineAndEndTimeIsNull(Machine machine);

    Optional<DowntimeEvent> findByMachineIdAndEndTimeIsNull(UUID machineId);

    List<DowntimeEvent> findByPlantIdAndIsDeletedFalse(UUID plantId);

    List<DowntimeEvent> findByMachineLineIdAndIsDeletedFalse(UUID lineId);

    @Query("SELECT d FROM DowntimeEvent d WHERE d.isDeleted = false " +
           "AND (:machineId IS NULL OR d.machine.id = :machineId) " +
           "AND (:openOnly IS NULL OR (:openOnly = true AND d.endTime IS NULL) OR (:openOnly = false AND d.endTime IS NOT NULL)) " +
           "AND (cast(:fromTime as timestamp) IS NULL OR d.startTime >= :fromTime) " +
           "AND (cast(:toTime as timestamp) IS NULL OR d.startTime < :toTime)")
    Page<DowntimeEvent> searchDowntimeEvents(
            @Param("machineId") UUID machineId,
            @Param("openOnly") Boolean openOnly,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            Pageable pageable
    );

    @Query("SELECT d FROM DowntimeEvent d WHERE d.isDeleted = false " +
           "AND d.startTime < :rangeTo " +
           "AND (d.endTime IS NULL OR d.endTime > :rangeFrom)")
    List<DowntimeEvent> findOverlappingEvents(
            @Param("rangeFrom") Instant rangeFrom,
            @Param("rangeTo") Instant rangeTo
    );

    @Query("SELECT d FROM DowntimeEvent d WHERE d.isDeleted = false " +
           "AND d.endTime IS NULL " +
           "AND d.rootCausePromptedAt IS NOT NULL " +
           "AND d.rootCauseAcknowledgedAt IS NULL " +
           "ORDER BY d.rootCausePromptedAt ASC")
    List<DowntimeEvent> findPendingRootCauses();

    @Query("SELECT d FROM DowntimeEvent d WHERE d.isDeleted = false " +
           "AND (:machineId IS NULL OR d.machine.id = :machineId) " +
           "AND (cast(:fromTime as timestamp) IS NULL OR d.startTime >= :fromTime) " +
           "AND (cast(:toTime as timestamp) IS NULL OR d.startTime < :toTime)")
    List<DowntimeEvent> findEventsInInterval(
            @Param("machineId") UUID machineId,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime
    );
}
