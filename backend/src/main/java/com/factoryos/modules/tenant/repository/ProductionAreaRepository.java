package com.factoryos.modules.tenant.repository;

import com.factoryos.modules.tenant.domain.ProductionArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductionAreaRepository extends JpaRepository<ProductionArea, UUID> {
    Optional<ProductionArea> findByIdAndIsDeletedFalse(UUID id);
    List<ProductionArea> findByPlantIdAndIsDeletedFalseOrderByCodeAsc(UUID plantId);
    boolean existsByPlantIdAndCodeIgnoreCaseAndIsDeletedFalse(UUID plantId, String code);
}
