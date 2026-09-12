package com.factoryos.modules.erp.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.erp.adapter.ErpClientAdapter;
import com.factoryos.modules.erp.domain.*;
import com.factoryos.modules.erp.dto.*;
import com.factoryos.modules.erp.repository.ErpConnectorRepository;
import com.factoryos.modules.erp.repository.ErpOrderConfirmationRepository;
import com.factoryos.modules.erp.repository.ErpSyncLogRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.repository.PlantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ErpSyncService {

    private static final Logger log = LoggerFactory.getLogger(ErpSyncService.class);

    private final ErpConnectorRepository connectorRepository;
    private final ErpOrderConfirmationRepository confirmationRepository;
    private final ErpSyncLogRepository syncLogRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final PlantRepository plantRepository;
    private final List<ErpClientAdapter> adapters;

    public ErpSyncService(
            ErpConnectorRepository connectorRepository,
            ErpOrderConfirmationRepository confirmationRepository,
            ErpSyncLogRepository syncLogRepository,
            ProductionOrderRepository productionOrderRepository,
            PlantRepository plantRepository,
            List<ErpClientAdapter> adapters) {
        this.connectorRepository = connectorRepository;
        this.confirmationRepository = confirmationRepository;
        this.syncLogRepository = syncLogRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.plantRepository = plantRepository;
        this.adapters = adapters;
    }

    private ErpClientAdapter getAdapter(ErpType type) {
        return adapters.stream()
                .filter(a -> a.getSupportedType() == type)
                .findFirst()
                .orElse(adapters.stream().filter(a -> a.getSupportedType() == ErpType.MOCK_ERP).findFirst()
                        .orElseThrow(() -> new IllegalStateException("No ERP adapter available for type: " + type)));
    }

    @Transactional(readOnly = true)
    public List<ErpConnectorDto> getAllConnectors() {
        return connectorRepository.findByIsDeletedFalse().stream().map(ErpConnectorDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ErpConnectorDto getConnectorById(UUID id) {
        return connectorRepository.findByIdAndIsDeletedFalse(id).map(ErpConnectorDto::from)
                .orElseThrow(() -> AppException.notFound("Connector not found: " + id));
    }

    @Transactional
    public ErpConnectorDto createConnector(CreateErpConnectorRequest req) {
        ErpConnector connector = new ErpConnector();
        if (req.getPlantId() != null) {
            plantRepository.findByIdAndIsDeletedFalse(req.getPlantId()).ifPresent(connector::setPlant);
        }
        connector.setName(req.getName());
        connector.setErpType(req.getErpType() != null ? req.getErpType() : ErpType.SAP_S4HANA);
        connector.setBaseUrl(req.getBaseUrl());
        connector.setAuthType(req.getAuthType() != null ? req.getAuthType() : "BASIC");
        connector.setApiKeyOrUser(req.getApiKeyOrUser());
        connector.setSecretOrToken(req.getSecretOrToken());
        connector.setClientId(req.getClientId());
        connector.setCompanyIdOrClient(req.getCompanyIdOrClient());
        connector.setSyncIntervalSeconds(req.getSyncIntervalSeconds() > 0 ? req.getSyncIntervalSeconds() : 300);
        connector.setActive(true);
        connector.setAutoSyncEnabled(req.isAutoSyncEnabled());
        connector.setHealthStatus("HEALTHY");

        return ErpConnectorDto.from(connectorRepository.save(connector));
    }

    @Transactional
    public boolean testConnector(UUID id) {
        ErpConnector connector = connectorRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("Connector not found: " + id));
        ErpClientAdapter adapter = getAdapter(connector.getErpType());
        boolean ok = adapter.testConnection(connector);
        connector.setHealthStatus(ok ? "HEALTHY" : "OFFLINE");
        connector.setLastHealthCheckAt(Instant.now());
        connectorRepository.save(connector);

        ErpSyncLog logEntry = new ErpSyncLog();
        logEntry.setConnector(connector);
        logEntry.setPlant(connector.getPlant());
        logEntry.setSyncDirection(ErpSyncDirection.HEALTH_CHECK);
        logEntry.setStatus(ok ? "SUCCESS" : "FAILURE");
        logEntry.setErrorMessage(ok ? null : "Connection check failed");
        logEntry.setSyncedAt(Instant.now());
        syncLogRepository.save(logEntry);

        return ok;
    }

    @Transactional
    public List<ProductionOrder> pollReleasedOrders(UUID connectorId) {
        ErpConnector connector = connectorRepository.findByIdAndIsDeletedFalse(connectorId)
                .orElseThrow(() -> AppException.notFound("Connector not found: " + connectorId));
        ErpClientAdapter adapter = getAdapter(connector.getErpType());
        List<InboundErpOrderDto> releasedOrders = adapter.fetchReleasedOrders(connector, connector.getLastSyncAt());

        List<ProductionOrder> createdOrUpdated = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;

        for (InboundErpOrderDto dto : releasedOrders) {
            try {
                ProductionOrder po = processInboundOrder(connector, dto);
                createdOrUpdated.add(po);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to process inbound ERP order {}: {}", dto.getErpOrderId(), e.getMessage());
                errorCount++;
            }
        }

        connector.setLastSyncAt(Instant.now());
        connector.setHealthStatus(errorCount == 0 ? "HEALTHY" : "DEGRADED");
        connectorRepository.save(connector);

        ErpSyncLog syncLog = new ErpSyncLog();
        syncLog.setConnector(connector);
        syncLog.setPlant(connector.getPlant());
        syncLog.setSyncDirection(ErpSyncDirection.INBOUND_RELEASE);
        syncLog.setStatus(errorCount == 0 ? "SUCCESS" : (successCount > 0 ? "PARTIAL" : "FAILURE"));
        syncLog.setPayloadJson("{\"count\":" + releasedOrders.size() + "}");
        syncLog.setResponseJson("{\"processed\":" + successCount + ",\"failed\":" + errorCount + "}");
        syncLog.setSyncedAt(Instant.now());
        syncLogRepository.save(syncLog);

        return createdOrUpdated;
    }

    @Transactional
    public ProductionOrder processInboundOrder(ErpConnector connector, InboundErpOrderDto dto) {
        Optional<ProductionOrder> existing = productionOrderRepository.findByErpOrderIdAndIsDeletedFalse(dto.getErpOrderId());
        ProductionOrder po;
        if (existing.isPresent()) {
            po = existing.get();
        } else {
            po = new ProductionOrder();
            po.setOrderNumber(dto.getOrderNumber() != null ? dto.getOrderNumber() : "PO-ERP-" + dto.getErpOrderId());
            po.setStatus(ProductionOrderStatus.DRAFT);
            po.setErpOrderId(dto.getErpOrderId());
            po.setErpSystem(connector.getErpType().name());
        }

        po.setProductCode(dto.getProductCode() != null ? dto.getProductCode() : "MAT-GENERIC");
        po.setProductDescription(dto.getProductDescription() != null ? dto.getProductDescription() : po.getProductCode());
        po.setPlannedQuantity(dto.getPlannedQuantity() > 0 ? dto.getPlannedQuantity() : 100);
        po.setErpBatchNumber(dto.getErpBatchNumber());
        po.setErpSyncStatus(ErpSyncStatus.SYNCED.name());
        po.setLastErpSyncAt(Instant.now());

        if (po.getPlant() == null) {
            if (connector.getPlant() != null) {
                po.setPlant(connector.getPlant());
            } else if (dto.getPlantCode() != null) {
                plantRepository.findByCodeIgnoreCaseAndIsDeletedFalse(dto.getPlantCode()).ifPresent(po::setPlant);
            }
        }

        return productionOrderRepository.save(po);
    }

    @Transactional
    public ErpOrderConfirmationDto submitOrderConfirmation(UUID orderId, int goodQty, int scrapQty, String scrapReason, double laborHours, double machineHours) {
        ProductionOrder po = productionOrderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> AppException.notFound("Order not found: " + orderId));

        ErpConnector connector = null;
        if (po.getPlant() != null) {
            List<ErpConnector> plantConnectors = connectorRepository.findByPlantIdAndIsDeletedFalse(po.getPlant().getId());
            if (!plantConnectors.isEmpty()) {
                connector = plantConnectors.get(0);
            }
        }
        if (connector == null) {
            List<ErpConnector> activeList = connectorRepository.findByIsActiveTrueAndIsDeletedFalse();
            if (!activeList.isEmpty()) {
                connector = activeList.get(0);
            }
        }

        ErpOrderConfirmation confirmation = new ErpOrderConfirmation();
        confirmation.setProductionOrder(po);
        confirmation.setConnector(connector);
        confirmation.setPlant(po.getPlant());
        confirmation.setConfirmationNumber("CONF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        confirmation.setErpOrderId(po.getErpOrderId() != null ? po.getErpOrderId() : po.getOrderNumber());
        confirmation.setConfirmedGoodQty(goodQty);
        confirmation.setConfirmedScrapQty(scrapQty);
        confirmation.setScrapReason(scrapReason);
        confirmation.setLaborHours(laborHours);
        confirmation.setMachineHours(machineHours);
        confirmation.setErpPostingStatus("PENDING");

        if (connector != null) {
            ErpClientAdapter adapter = getAdapter(connector.getErpType());
            ErpClientAdapter.ErpConfirmationResult result = adapter.postConfirmation(connector, confirmation);

            if (result.isSuccess()) {
                confirmation.setErpPostingStatus("POSTED");
                confirmation.setErpDocumentNumber(result.getErpDocumentNumber());
                confirmation.setPostedAt(Instant.now());

                po.setErpSyncStatus(ErpSyncStatus.CONFIRMED.name());
                po.setLastErpSyncAt(Instant.now());
                productionOrderRepository.save(po);
            } else {
                confirmation.setErpPostingStatus("FAILED");
                confirmation.setErrorMessage(result.getErrorMessage());
                confirmation.setRetryCount(1);

                po.setErpSyncStatus(ErpSyncStatus.SYNC_ERROR.name());
                productionOrderRepository.save(po);
            }
        }

        ErpOrderConfirmation savedConf = confirmationRepository.save(confirmation);

        ErpSyncLog syncLog = new ErpSyncLog();
        syncLog.setConnector(connector);
        syncLog.setPlant(po.getPlant());
        syncLog.setSyncDirection(ErpSyncDirection.OUTBOUND_CONFIRMATION);
        syncLog.setEntityType("PRODUCTION_ORDER");
        syncLog.setEntityId(po.getId());
        syncLog.setErpReferenceId(confirmation.getErpOrderId());
        syncLog.setStatus("POSTED".equals(confirmation.getErpPostingStatus()) ? "SUCCESS" : "FAILURE");
        syncLog.setErrorMessage(confirmation.getErrorMessage());
        syncLog.setSyncedAt(Instant.now());
        syncLogRepository.save(syncLog);

        return ErpOrderConfirmationDto.from(savedConf);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void retryPendingConfirmations() {
        List<ErpOrderConfirmation> pending = confirmationRepository.findByErpPostingStatusAndIsDeletedFalse("FAILED");
        for (ErpOrderConfirmation conf : pending) {
            if (conf.getRetryCount() >= 5) {
                continue;
            }
            ErpConnector connector = conf.getConnector();
            if (connector == null || !connector.isActive()) {
                continue;
            }
            ErpClientAdapter adapter = getAdapter(connector.getErpType());
            ErpClientAdapter.ErpConfirmationResult result = adapter.postConfirmation(connector, conf);
            if (result.isSuccess()) {
                conf.setErpPostingStatus("POSTED");
                conf.setErpDocumentNumber(result.getErpDocumentNumber());
                conf.setPostedAt(Instant.now());
                conf.setErrorMessage(null);
            } else {
                conf.setRetryCount(conf.getRetryCount() + 1);
                conf.setErrorMessage(result.getErrorMessage());
            }
            confirmationRepository.save(conf);
        }
    }

    @Transactional(readOnly = true)
    public List<ErpSyncLogDto> getRecentSyncLogs() {
        return syncLogRepository.findTop50ByOrderBySyncedAtDesc().stream().map(ErpSyncLogDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ErpOrderConfirmationDto> getRecentConfirmations() {
        return confirmationRepository.findTop50ByIsDeletedFalseOrderByCreatedAtDesc().stream().map(ErpOrderConfirmationDto::from).toList();
    }
}
