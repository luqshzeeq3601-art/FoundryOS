package com.factoryos.modules.tenant.repository;

import com.factoryos.modules.tenant.domain.WorkCell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkCellRepository extends JpaRepository<WorkCell, UUID> {
    Optional<WorkCell> findByIdAndIsDeletedFalse(UUID id);
    List<WorkCell> findByLineIdAndIsDeletedFalseOrderByCodeAsc(UUID lineId);
    boolean existsByLineIdAndCodeIgnoreCaseAndIsDeletedFalse(UUID lineId, String code);
}
