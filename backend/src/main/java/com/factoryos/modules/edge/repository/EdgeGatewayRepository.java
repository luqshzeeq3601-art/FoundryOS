package com.factoryos.modules.edge.repository;

import com.factoryos.modules.edge.domain.EdgeGateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EdgeGatewayRepository extends JpaRepository<EdgeGateway, UUID> {

    Optional<EdgeGateway> findByIdAndIsDeletedFalse(UUID id);

    Optional<EdgeGateway> findByGatewayCodeAndIsDeletedFalse(String gatewayCode);

    List<EdgeGateway> findByPlantIdAndIsDeletedFalse(UUID plantId);

    List<EdgeGateway> findAllByIsDeletedFalseOrderByGatewayCodeAsc();

    boolean existsByGatewayCodeAndIsDeletedFalse(String gatewayCode);
}
