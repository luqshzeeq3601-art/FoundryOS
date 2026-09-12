package com.factoryos.modules.maintenance.repository;

import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.maintenance.domain.MaintenancePriority;
import com.factoryos.modules.maintenance.domain.MaintenanceStatus;
import com.factoryos.modules.maintenance.domain.MaintenanceWorkOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaintenanceWorkOrderRepository extends JpaRepository<MaintenanceWorkOrder, UUID> {

    Optional<MaintenanceWorkOrder> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByWorkOrderNumberAndIsDeletedFalse(String workOrderNumber);

    @Query("SELECT w FROM MaintenanceWorkOrder w WHERE w.isDeleted = false " +
           "AND (:machineId IS NULL OR w.machine.id = :machineId) " +
           "AND (:priority IS NULL OR w.priority = :priority) " +
           "AND (:status IS NULL OR w.status = :status) " +
           "AND (:isPrescriptive IS NULL OR w.isPrescriptive = :isPrescriptive) " +
           "AND (:assignedToId IS NULL OR (w.assignedTo IS NOT NULL AND w.assignedTo.id = :assignedToId)) " +
           "AND (:search IS NULL OR LOWER(w.workOrderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(w.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MaintenanceWorkOrder> searchWorkOrders(
            @Param("machineId") UUID machineId,
            @Param("priority") MaintenancePriority priority,
            @Param("status") MaintenanceStatus status,
            @Param("isPrescriptive") Boolean isPrescriptive,
            @Param("assignedToId") UUID assignedToId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT w FROM MaintenanceWorkOrder w WHERE w.isDeleted = false " +
           "AND w.machine.id = :machineId " +
           "AND w.isPrescriptive = true " +
           "AND w.status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS') " +
           "AND (w.createdAt >= :since OR (w.lastTriggeredAt IS NOT NULL AND w.lastTriggeredAt >= :since)) " +
           "ORDER BY w.createdAt DESC")
    List<MaintenanceWorkOrder> findActivePrescriptiveOrdersForMachineSince(
            @Param("machineId") UUID machineId,
            @Param("since") java.time.Instant since
    );

    @Query("SELECT COUNT(w) FROM MaintenanceWorkOrder w WHERE w.isDeleted = false " +
           "AND w.assignedTo = :user " +
           "AND w.status IN :statuses")
    long countByAssignedToAndStatusIn(
            @Param("user") User user,
            @Param("statuses") List<MaintenanceStatus> statuses
    );

    @Query("SELECT COUNT(w) FROM MaintenanceWorkOrder w WHERE w.isDeleted = false AND w.status = :status")
    long countByStatus(@Param("status") MaintenanceStatus status);
}
