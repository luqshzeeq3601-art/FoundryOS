package com.factoryos.modules.tenant.repository;

import com.factoryos.modules.tenant.domain.UserPlantMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPlantMembershipRepository extends JpaRepository<UserPlantMembership, UUID> {
    List<UserPlantMembership> findByUserId(UUID userId);
    List<UserPlantMembership> findByPlantId(UUID plantId);
    Optional<UserPlantMembership> findByUserIdAndPlantId(UUID userId, UUID plantId);
    Optional<UserPlantMembership> findByUserIdAndIsDefaultTrue(UUID userId);
    boolean existsByUserIdAndPlantId(UUID userId, UUID plantId);
    void deleteByUserIdAndPlantId(UUID userId, UUID plantId);
}
