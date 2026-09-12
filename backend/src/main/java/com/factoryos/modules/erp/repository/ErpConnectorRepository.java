package com.factoryos.modules.erp.repository;

import com.factoryos.modules.erp.domain.ErpConnector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ErpConnectorRepository extends JpaRepository<ErpConnector, UUID> {

    Optional<ErpConnector> findByIdAndIsDeletedFalse(UUID id);

    List<ErpConnector> findByIsDeletedFalse();

    List<ErpConnector> findByPlantIdAndIsDeletedFalse(UUID plantId);

    List<ErpConnector> findByIsActiveTrueAndIsDeletedFalse();
}