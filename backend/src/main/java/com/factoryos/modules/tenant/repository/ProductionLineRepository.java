package com.factoryos.modules.tenant.repository;

import com.factoryos.modules.tenant.domain.ProductionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductionLineRepository extends JpaRepository<ProductionLine, UUID> {
    Optional<ProductionLine> findByIdAndIsDeletedFalse(UUID id);
    List<ProductionLine> findByAreaIdAndIsDeletedFalseOrderByCodeAsc(UUID areaId);
    List<ProductionLine> findByAreaPlantIdAndIsDeletedFalseOrderByCodeAsc(UUID plantId);
    boolean existsByAreaIdAndCodeIgnoreCaseAndIsDeletedFalse(UUID areaId, String code);
}
