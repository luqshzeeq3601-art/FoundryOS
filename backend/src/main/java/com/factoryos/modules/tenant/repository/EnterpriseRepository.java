package com.factoryos.modules.tenant.repository;

import com.factoryos.modules.tenant.domain.Enterprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnterpriseRepository extends JpaRepository<Enterprise, UUID> {
    Optional<Enterprise> findByIdAndIsDeletedFalse(UUID id);
    Optional<Enterprise> findByCodeIgnoreCaseAndIsDeletedFalse(String code);
    List<Enterprise> findAllByIsDeletedFalseOrderByCodeAsc();
    boolean existsByCodeIgnoreCaseAndIsDeletedFalse(String code);
}
