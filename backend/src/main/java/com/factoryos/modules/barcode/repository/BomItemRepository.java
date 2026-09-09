package com.factoryos.modules.barcode.repository;

import com.factoryos.modules.barcode.domain.BomItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BomItemRepository extends JpaRepository<BomItem, UUID> {
    List<BomItem> findByProductCode(String productCode);
    List<BomItem> findByProductCodeIgnoreCase(String productCode);
    Optional<BomItem> findByProductCodeAndMaterialCode(String productCode, String materialCode);
}
