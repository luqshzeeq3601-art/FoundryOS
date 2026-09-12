package com.factoryos.modules.vibration.repository;

import com.factoryos.modules.vibration.domain.VibrationBurstSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VibrationBurstSampleRepository extends JpaRepository<VibrationBurstSample, UUID> {

    Optional<VibrationBurstSample> findFirstByMachineIdAndIsDeletedFalseOrderByCapturedAtDesc(UUID machineId);

    List<VibrationBurstSample> findTop10ByMachineIdAndIsDeletedFalseOrderByCapturedAtDesc(UUID machineId);

    @Query("SELECT b FROM VibrationBurstSample b WHERE b.plant.id = :plantId AND b.isDeleted = false ORDER BY b.capturedAt DESC")
    List<VibrationBurstSample> findRecentByPlant(@Param("plantId") UUID plantId);
}
