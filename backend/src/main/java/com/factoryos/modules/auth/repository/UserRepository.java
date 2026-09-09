package com.factoryos.modules.auth.repository;

import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    Optional<User> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByEmailAndIsDeletedFalse(String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName AND u.isActive = true AND u.isDeleted = false")
    long countActiveUsersByRole(@Param("roleName") RoleType roleName);

    @Query("SELECT u FROM User u WHERE u.isDeleted = false " +
           "AND (:roleName IS NULL OR u.role.name = :roleName) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive) " +
           "AND (:search IS NULL OR lower(u.displayName) LIKE lower(concat('%', :search, '%')) OR lower(u.email) LIKE lower(concat('%', :search, '%')))")
    Page<User> searchUsers(
            @Param("roleName") RoleType roleName,
            @Param("isActive") Boolean isActive,
            @Param("search") String search,
            Pageable pageable
    );
}
