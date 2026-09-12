package com.factoryos.modules.sop.repository;

import com.factoryos.modules.sop.domain.SopExecutionSession;
import com.factoryos.modules.sop.domain.SopSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SopExecutionSessionRepository extends JpaRepository<SopExecutionSession, UUID> {

    List<SopExecutionSession> findByProductionOrderIdOrderByStartedAtDesc(UUID productionOrderId);

    Optional<SopExecutionSession> findFirstByProductionOrderIdAndSessionStatusOrderByStartedAtDesc(UUID productionOrderId, SopSessionStatus status);

    Optional<SopExecutionSession> findFirstByProductionOrderIdOrderByStartedAtDesc(UUID productionOrderId);

    List<SopExecutionSession> findByPlantIdOrderByStartedAtDesc(UUID plantId);

    List<SopExecutionSession> findTop50ByOrderByStartedAtDesc();
}
