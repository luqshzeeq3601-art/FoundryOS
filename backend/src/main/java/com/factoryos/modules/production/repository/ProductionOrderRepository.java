package com.factoryos.modules.production.repository;

import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, UUID> {

    Optional<ProductionOrder> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByOrderNumberAndIsDeletedFalse(String orderNumber);

    Optional<ProductionOrder> findByMachineAndStatus(Machine machine, ProductionOrderStatus status);

    Optional<ProductionOrder> findByMachineIdAndStatus(UUID machineId, ProductionOrderStatus status);

    @Query("SELECT p FROM ProductionOrder p WHERE p.isDeleted = false " +
           "AND (:machineId IS NULL OR p.machine.id = :machineId) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:search IS NULL OR LOWER(p.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ProductionOrder> searchOrders(
            @Param("machineId") UUID machineId,
            @Param("status") ProductionOrderStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT p FROM ProductionOrder p WHERE p.isDeleted = false AND p.status = 'IN_PROGRESS'")
    List<ProductionOrder> findAllActiveOrders();

    @Query("SELECT COUNT(p) FROM ProductionOrder p WHERE p.isDeleted = false AND p.status = :status")
    long countByStatus(@Param("status") ProductionOrderStatus status);
}
