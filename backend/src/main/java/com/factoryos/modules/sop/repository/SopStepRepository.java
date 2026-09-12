package com.factoryos.modules.sop.repository;

import com.factoryos.modules.sop.domain.SopStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SopStepRepository extends JpaRepository<SopStep, UUID> {

    List<SopStep> findBySopIdOrderByStepNumberAsc(UUID sopId);

    Optional<SopStep> findBySopIdAndStepNumber(UUID sopId, int stepNumber);

    long countBySopId(UUID sopId);
}
