package com.factoryos.modules.telemetry.repository;

import com.factoryos.modules.telemetry.domain.MachineTelemetryPoint;
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
public interface MachineTelemetryRepository extends JpaRepository<MachineTelemetryPoint, UUID> {

    List<MachineTelemetryPoint> findByMachineIdOrderByTimestampDesc(UUID machineId, Pageable pageable);

    @Query("SELECT mtp FROM MachineTelemetryPoint mtp WHERE mtp.machine.id = :machineId AND mtp.tagName = :tagName ORDER BY mtp.timestamp DESC LIMIT 1")
    Optional<MachineTelemetryPoint> findLatestByMachineIdAndTagName(@Param("machineId") UUID machineId, @Param("tagName") String tagName);

    @Query("SELECT mtp FROM MachineTelemetryPoint mtp WHERE mtp.machine.id = :machineId AND mtp.timestamp >= :since ORDER BY mtp.timestamp ASC")
    List<MachineTelemetryPoint> findByMachineIdAndTimestampAfter(@Param("machineId") UUID machineId, @Param("since") Instant since);
}
