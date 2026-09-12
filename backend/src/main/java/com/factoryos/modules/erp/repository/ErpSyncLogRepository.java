package com.factoryos.modules.erp.repository;

import com.factoryos.modules.erp.domain.ErpSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ErpSyncLogRepository extends JpaRepository<ErpSyncLog, UUID> {

    List<ErpSyncLog> findTop50ByOrderBySyncedAtDesc();

    List<ErpSyncLog> findTop50ByPlantIdOrderBySyncedAtDesc(UUID plantId);

    List<ErpSyncLog> findTop50ByConnectorIdOrderBySyncedAtDesc(UUID connectorId);
}