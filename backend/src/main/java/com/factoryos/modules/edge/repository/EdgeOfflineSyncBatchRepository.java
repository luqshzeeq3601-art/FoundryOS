package com.factoryos.modules.edge.repository;

import com.factoryos.modules.edge.domain.EdgeOfflineSyncBatch;
import com.factoryos.modules.edge.domain.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EdgeOfflineSyncBatchRepository extends JpaRepository<EdgeOfflineSyncBatch, UUID> {

    Optional<EdgeOfflineSyncBatch> findByBatchId(String batchId);

    List<EdgeOfflineSyncBatch> findByGatewayIdOrderByCreatedAtDesc(UUID gatewayId);

    List<EdgeOfflineSyncBatch> findByPlantIdOrderByCreatedAtDesc(UUID plantId);

    List<EdgeOfflineSyncBatch> findTop50ByOrderByCreatedAtDesc();

    List<EdgeOfflineSyncBatch> findBySyncStatusOrderByCreatedAtDesc(SyncStatus syncStatus);
}
