package com.factoryos.modules.materials.repository;

import com.factoryos.modules.materials.domain.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID> {

    Optional<Material> findByIdAndIsDeletedFalse(UUID id);

    Optional<Material> findByMaterialCodeIgnoreCaseAndIsDeletedFalse(String materialCode);

    List<Material> findByIsDeletedFalseOrderByMaterialCodeAsc();

    List<Material> findByPlantIdAndIsDeletedFalseOrderByMaterialCodeAsc(UUID plantId);
}
