package com.factoryos.modules.barcode.repository;

import com.factoryos.modules.barcode.domain.BarcodeScanLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BarcodeScanLogRepository extends JpaRepository<BarcodeScanLog, UUID> {
    List<BarcodeScanLog> findTop20ByOrderByCreatedAtDesc();
    Page<BarcodeScanLog> findByMachineIdOrderByCreatedAtDesc(UUID machineId, Pageable pageable);
    Page<BarcodeScanLog> findByProductionOrderIdOrderByCreatedAtDesc(UUID productionOrderId, Pageable pageable);
}
