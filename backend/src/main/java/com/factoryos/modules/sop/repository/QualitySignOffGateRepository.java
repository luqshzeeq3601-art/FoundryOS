package com.factoryos.modules.sop.repository;

import com.factoryos.modules.sop.domain.GateStatus;
import com.factoryos.modules.sop.domain.QualitySignOffGate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QualitySignOffGateRepository extends JpaRepository<QualitySignOffGate, UUID> {

    Optional<QualitySignOffGate> findByProductionOrderId(UUID productionOrderId);

    List<QualitySignOffGate> findByGateStatus(GateStatus gateStatus);

    boolean existsByProductionOrderId(UUID productionOrderId);
}
