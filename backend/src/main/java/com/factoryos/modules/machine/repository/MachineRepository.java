package com.factoryos.modules.machine.repository;

import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
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
public interface MachineRepository extends JpaRepository<Machine, UUID> {

    Optional<Machine> findByIdAndIsDeletedFalse(UUID id);

    Optional<Machine> findBySerialNumberIgnoreCaseAndIsDeletedFalse(String serialNumber);

    Optional<Machine> findByNameIgnoreCaseAndIsDeletedFalse(String name);

    boolean existsBySerialNumberAndIsDeletedFalse(String serialNumber);

    long countByStatusAndIsDeletedFalse(MachineStatus status);

    long countByIsDeletedFalse();

    List<Machine> findByPlantIdAndIsDeletedFalse(UUID plantId);

    List<Machine> findByLineIdAndIsDeletedFalse(UUID lineId);

    List<Machine> findByWorkCellIdAndIsDeletedFalse(UUID workCellId);

    @Query("SELECT m FROM Machine m WHERE m.isDeleted = false " +
           "AND (:plantId IS NULL OR m.plant.id = :plantId) " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:search IS NULL OR lower(m.name) LIKE lower(concat('%', :search, '%')) OR lower(m.serialNumber) LIKE lower(concat('%', :search, '%')) OR lower(m.location) LIKE lower(concat('%', :search, '%')))")
    Page<Machine> searchMachinesWithPlant(
            @Param("plantId") UUID plantId,
            @Param("status") MachineStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT m FROM Machine m WHERE m.isDeleted = false " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:search IS NULL OR lower(m.name) LIKE lower(concat('%', :search, '%')) OR lower(m.serialNumber) LIKE lower(concat('%', :search, '%')) OR lower(m.location) LIKE lower(concat('%', :search, '%')))")
    Page<Machine> searchMachines(
            @Param("status") MachineStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    List<Machine> findAllByIsDeletedFalse();
}
