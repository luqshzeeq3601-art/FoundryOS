package com.factoryos.modules.barcode.repository;

import com.factoryos.modules.barcode.domain.MaterialLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialLotRepository extends JpaRepository<MaterialLot, UUID> {
    Optional<MaterialLot> findByLotNumber(String lotNumber);
    Optional<MaterialLot> findByLotNumberIgnoreCase(String lotNumber);
}
