package com.factoryos.modules.tenant.repository;

import com.factoryos.modules.tenant.domain.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlantRepository extends JpaRepository<Plant, UUID> {
    Optional<Plant> findByIdAndIsDeletedFalse(UUID id);
    Optional<Plant> findByCodeIgnoreCaseAndIsDeletedFalse(String code);
    List<Plant> findAllByIsDeletedFalseOrderByCodeAsc();
    List<Plant> findByEnterpriseIdAndIsDeletedFalseOrderByCodeAsc(UUID enterpriseId);
    List<Plant> findByIdInAndIsDeletedFalse(List<UUID> ids);
    boolean existsByCodeIgnoreCaseAndIsDeletedFalse(String code);
}
