package com.factoryos.modules.edge.repository;

import com.factoryos.modules.edge.domain.EdgeOfflineTransactionLog;
import com.factoryos.modules.edge.domain.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EdgeOfflineTransactionLogRepository extends JpaRepository<EdgeOfflineTransactionLog, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<EdgeOfflineTransactionLog> findByIdempotencyKey(String idempotencyKey);

    List<EdgeOfflineTransactionLog> findByBatchIdOrderBySequenceIdAsc(UUID batchId);

    List<EdgeOfflineTransactionLog> findTop100ByOrderBySyncedAtDesc();

    long countByExecutionStatus(ExecutionStatus status);
}
