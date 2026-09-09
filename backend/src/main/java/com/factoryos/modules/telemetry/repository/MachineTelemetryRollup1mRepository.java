package com.factoryos.modules.telemetry.repository;

import com.factoryos.modules.telemetry.domain.MachineTelemetryRollup1m;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MachineTelemetryRollup1mRepository extends JpaRepository<MachineTelemetryRollup1m, UUID> {

    @Query("SELECT r FROM MachineTelemetryRollup1m r WHERE r.machine.id = :machineId AND r.tagName = :tagName AND r.bucketStart >= :from AND r.bucketStart <= :to ORDER BY r.bucketStart ASC")
    List<MachineTelemetryRollup1m> findSeries(
            @Param("machineId") UUID machineId,
            @Param("tagName") String tagName,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Modifying
    @Query("DELETE FROM MachineTelemetryRollup1m r WHERE r.bucketStart < :cutoff")
    int pruneOlderThan(@Param("cutoff") Instant cutoff);
}
