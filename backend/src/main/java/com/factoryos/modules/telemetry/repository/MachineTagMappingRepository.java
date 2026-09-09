package com.factoryos.modules.telemetry.repository;

import com.factoryos.modules.telemetry.domain.MachineTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MachineTagMappingRepository extends JpaRepository<MachineTagMapping, UUID> {

    List<MachineTagMapping> findByMachineIdAndIsDeletedFalse(UUID machineId);

    Optional<MachineTagMapping> findByMachineIdAndTagNameAndIsDeletedFalse(UUID machineId, String tagName);

    boolean existsByMachineIdAndTagNameAndIsDeletedFalse(UUID machineId, String tagName);

    @Query("SELECT mtm FROM MachineTagMapping mtm WHERE mtm.machine.id = :machineId AND mtm.isActive = true AND mtm.isDeleted = false")
    List<MachineTagMapping> findActiveMappingsByMachineId(@Param("machineId") UUID machineId);
}
