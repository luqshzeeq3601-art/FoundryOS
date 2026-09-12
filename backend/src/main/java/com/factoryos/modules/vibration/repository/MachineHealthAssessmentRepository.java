package com.factoryos.modules.vibration.repository;

import com.factoryos.modules.vibration.domain.MachineHealthAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MachineHealthAssessmentRepository extends JpaRepository<MachineHealthAssessment, UUID> {

    Optional<MachineHealthAssessment> findFirstByMachineIdAndIsDeletedFalseOrderByAssessedAtDesc(UUID machineId);

    List<MachineHealthAssessment> findTop20ByMachineIdAndIsDeletedFalseOrderByAssessedAtDesc(UUID machineId);

    @Query("SELECT a FROM MachineHealthAssessment a WHERE a.isDeleted = false AND a.assessedAt IN (" +
           "  SELECT MAX(a2.assessedAt) FROM MachineHealthAssessment a2 WHERE a2.isDeleted = false GROUP BY a2.machine.id" +
           ") ORDER BY a.healthScore ASC")
    List<MachineHealthAssessment> findLatestAssessmentsForAllMachines();

    @Query("SELECT a FROM MachineHealthAssessment a WHERE a.plant.id = :plantId AND a.isDeleted = false AND a.assessedAt IN (" +
           "  SELECT MAX(a2.assessedAt) FROM MachineHealthAssessment a2 WHERE a2.plant.id = :plantId AND a2.isDeleted = false GROUP BY a2.machine.id" +
           ") ORDER BY a.healthScore ASC")
    List<MachineHealthAssessment> findLatestAssessmentsByPlant(@Param("plantId") UUID plantId);
}
