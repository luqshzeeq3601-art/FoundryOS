package com.factoryos.modules.sop.repository;

import com.factoryos.modules.sop.domain.SopStepExecutionRecord;
import com.factoryos.modules.sop.domain.StepRecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SopStepExecutionRecordRepository extends JpaRepository<SopStepExecutionRecord, UUID> {

    List<SopStepExecutionRecord> findBySessionIdOrderByStepNumberAsc(UUID sessionId);

    Optional<SopStepExecutionRecord> findBySessionIdAndStepId(UUID sessionId, UUID stepId);

    Optional<SopStepExecutionRecord> findBySessionIdAndStepNumber(UUID sessionId, int stepNumber);

    long countBySessionIdAndStatus(UUID sessionId, StepRecordStatus status);
}
