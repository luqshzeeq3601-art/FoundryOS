package com.factoryos.modules.erp.repository;

import com.factoryos.modules.erp.domain.ErpOrderConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ErpOrderConfirmationRepository extends JpaRepository<ErpOrderConfirmation, UUID> {

    Optional<ErpOrderConfirmation> findByIdAndIsDeletedFalse(UUID id);

    Optional<ErpOrderConfirmation> findByConfirmationNumber(String confirmationNumber);

    List<ErpOrderConfirmation> findByProductionOrderIdAndIsDeletedFalse(UUID productionOrderId);

    List<ErpOrderConfirmation> findByPlantIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID plantId);

    List<ErpOrderConfirmation> findTop50ByIsDeletedFalseOrderByCreatedAtDesc();

    List<ErpOrderConfirmation> findByErpPostingStatusAndIsDeletedFalse(String status);
}