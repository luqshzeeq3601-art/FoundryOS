package com.factoryos.modules.sop.repository;

import com.factoryos.modules.sop.domain.SopCategory;
import com.factoryos.modules.sop.domain.SopStatus;
import com.factoryos.modules.sop.domain.StandardOperatingProcedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StandardOperatingProcedureRepository extends JpaRepository<StandardOperatingProcedure, UUID> {

    Optional<StandardOperatingProcedure> findByIdAndIsDeletedFalse(UUID id);

    Optional<StandardOperatingProcedure> findBySopCodeAndIsDeletedFalse(String sopCode);

    List<StandardOperatingProcedure> findByProductCodeAndIsDeletedFalse(String productCode);

    List<StandardOperatingProcedure> findByProductCodeIgnoreCaseAndIsDeletedFalse(String productCode);

    List<StandardOperatingProcedure> findByStatusAndIsDeletedFalse(SopStatus status);

    List<StandardOperatingProcedure> findAllByIsDeletedFalseOrderBySopCodeAsc();

    boolean existsBySopCodeAndIsDeletedFalse(String sopCode);

    @Query("SELECT s FROM StandardOperatingProcedure s WHERE s.isDeleted = false " +
           "AND (:plantId IS NULL OR s.plant.id = :plantId OR s.plant IS NULL) " +
           "AND (:category IS NULL OR s.category = :category) " +
           "AND (:productCode IS NULL OR LOWER(s.productCode) = LOWER(:productCode) OR s.productCode = '*')")
    List<StandardOperatingProcedure> searchSops(
            @Param("plantId") UUID plantId,
            @Param("category") SopCategory category,
            @Param("productCode") String productCode
    );
}
