package com.factoryos.modules.materials.repository;

import com.factoryos.modules.materials.domain.MaterialConsumptionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaterialConsumptionRecordRepository extends JpaRepository<MaterialConsumptionRecord, UUID> {

    List<MaterialConsumptionRecord> findByProductionOrderIdOrderByRecordedAtDesc(UUID productionOrderId);

    List<MaterialConsumptionRecord> findTop50ByOrderByRecordedAtDesc();

    List<MaterialConsumptionRecord> findByVarianceAlertTriggeredTrueOrderByRecordedAtDesc();

    List<MaterialConsumptionRecord> findTop50ByPlantIdOrderByRecordedAtDesc(UUID plantId);
}
