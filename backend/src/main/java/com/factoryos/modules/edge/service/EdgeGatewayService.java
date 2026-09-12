package com.factoryos.modules.edge.service;

import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.barcode.domain.BomItem;
import com.factoryos.modules.barcode.domain.MaterialLot;
import com.factoryos.modules.barcode.repository.BomItemRepository;
import com.factoryos.modules.barcode.repository.MaterialLotRepository;
import com.factoryos.modules.edge.domain.EdgeGateway;
import com.factoryos.modules.edge.dto.*;
import com.factoryos.modules.edge.repository.EdgeGatewayRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.repository.PlantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EdgeGatewayService {

    private static final Logger log = LoggerFactory.getLogger(EdgeGatewayService.class);

    private final EdgeGatewayRepository edgeGatewayRepository;
    private final PlantRepository plantRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final MachineRepository machineRepository;
    private final BomItemRepository bomItemRepository;
    private final MaterialLotRepository materialLotRepository;
    private final AuditRecordingService auditRecordingService;

    public EdgeGatewayService(EdgeGatewayRepository edgeGatewayRepository,
                              PlantRepository plantRepository,
                              ProductionOrderRepository productionOrderRepository,
                              MachineRepository machineRepository,
                              BomItemRepository bomItemRepository,
                              MaterialLotRepository materialLotRepository,
                              AuditRecordingService auditRecordingService) {
        this.edgeGatewayRepository = edgeGatewayRepository;
        this.plantRepository = plantRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.machineRepository = machineRepository;
        this.bomItemRepository = bomItemRepository;
        this.materialLotRepository = materialLotRepository;
        this.auditRecordingService = auditRecordingService;
    }

    public List<EdgeGatewayDto> getAllGateways() {
        return edgeGatewayRepository.findAllByIsDeletedFalseOrderByGatewayCodeAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<EdgeGatewayDto> getGatewaysByPlant(UUID plantId) {
        return edgeGatewayRepository.findByPlantIdAndIsDeletedFalse(plantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public EdgeGatewayDto getGatewayByCode(String gatewayCode) {
        EdgeGateway gateway = edgeGatewayRepository.findByGatewayCodeAndIsDeletedFalse(gatewayCode.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Edge Gateway not found with code: " + gatewayCode));
        return toDto(gateway);
    }

    @Transactional
    public EdgeGatewayDto createGateway(EdgeGatewayCreateRequestDto dto) {
        if (edgeGatewayRepository.existsByGatewayCodeAndIsDeletedFalse(dto.getGatewayCode().toUpperCase())) {
            throw new IllegalArgumentException("Gateway code already exists: " + dto.getGatewayCode());
        }

        Plant plant = plantRepository.findByIdAndIsDeletedFalse(dto.getPlantId())
                .orElseThrow(() -> new IllegalArgumentException("Plant not found with ID: " + dto.getPlantId()));

        EdgeGateway gateway = new EdgeGateway();
        gateway.setGatewayCode(dto.getGatewayCode().toUpperCase());
        gateway.setName(dto.getName());
        gateway.setPlant(plant);
        gateway.setIpAddress(dto.getIpAddress());
        gateway.setMacAddress(dto.getMacAddress());
        gateway.setStatus("ONLINE");
        gateway.setBufferCapacityRecords(dto.getBufferCapacityRecords() > 0 ? dto.getBufferCapacityRecords() : 100000);
        gateway.setFirmwareVersion(dto.getFirmwareVersion() != null ? dto.getFirmwareVersion() : "2.0.0-EDGE");
        gateway.setLastHeartbeatAt(Instant.now());

        EdgeGateway saved = edgeGatewayRepository.save(gateway);
        log.info("Created new Edge Gateway: {} for plant {}", saved.getGatewayCode(), plant.getName());

        return toDto(saved);
    }

    @Transactional
    public EdgeGatewayDto recordHeartbeat(String gatewayCode, EdgeGatewayHeartbeatRequestDto dto) {
        EdgeGateway gateway = edgeGatewayRepository.findByGatewayCodeAndIsDeletedFalse(gatewayCode.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Edge Gateway not found with code: " + gatewayCode));

        gateway.setLastHeartbeatAt(Instant.now());
        if (dto != null) {
            if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
                gateway.setStatus(dto.getStatus().toUpperCase());
            }
            if (dto.getFirmwareVersion() != null && !dto.getFirmwareVersion().isBlank()) {
                gateway.setFirmwareVersion(dto.getFirmwareVersion());
            }
            if (dto.getIpAddress() != null && !dto.getIpAddress().isBlank()) {
                gateway.setIpAddress(dto.getIpAddress());
            }
        }
        gateway.setUpdatedAt(Instant.now());

        EdgeGateway saved = edgeGatewayRepository.save(gateway);
        return toDto(saved);
    }

    public EdgeOfflineCacheManifestDto generateOfflineCacheManifest(String gatewayCode) {
        EdgeGateway gateway = edgeGatewayRepository.findByGatewayCodeAndIsDeletedFalse(gatewayCode.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Edge Gateway not found with code: " + gatewayCode));

        UUID plantId = gateway.getPlant().getId();
        EdgeOfflineCacheManifestDto manifest = new EdgeOfflineCacheManifestDto();
        manifest.setGatewayCode(gateway.getGatewayCode());
        manifest.setPlantId(plantId);
        manifest.setPlantName(gateway.getPlant().getName());
        manifest.setManifestGeneratedAt(Instant.now());

        // 1. Cached Orders for this plant
        List<ProductionOrder> orders = productionOrderRepository.findByPlantIdAndIsDeletedFalse(plantId);
        List<EdgeOfflineCacheManifestDto.CachedOrderDto> cachedOrders = orders.stream().map(order -> {
            EdgeOfflineCacheManifestDto.CachedOrderDto o = new EdgeOfflineCacheManifestDto.CachedOrderDto();
            o.setId(order.getId());
            o.setOrderNumber(order.getOrderNumber());
            o.setProductCode(order.getProductCode());
            o.setProductName(order.getProductDescription() != null ? order.getProductDescription() : order.getProductCode());
            o.setTargetQuantity(order.getPlannedQuantity());
            o.setGoodQuantity(order.getGoodQuantity());
            o.setScrapQuantity(order.getScrapQuantity());
            o.setStatus(order.getStatus().name());
            if (order.getMachine() != null) {
                o.setMachineId(order.getMachine().getId());
                o.setMachineCode(order.getMachine().getSerialNumber());
            }
            return o;
        }).collect(Collectors.toList());
        manifest.setActiveOrders(cachedOrders);

        // 2. Cached Machines for this plant
        List<Machine> machines = machineRepository.findByPlantIdAndIsDeletedFalse(plantId);
        List<EdgeOfflineCacheManifestDto.CachedMachineDto> cachedMachines = machines.stream().map(m -> {
            EdgeOfflineCacheManifestDto.CachedMachineDto cm = new EdgeOfflineCacheManifestDto.CachedMachineDto();
            cm.setId(m.getId());
            cm.setMachineCode(m.getSerialNumber());
            cm.setName(m.getName());
            cm.setStatus(m.getStatus().name());
            if (m.getLine() != null) {
                cm.setLineId(m.getLine().getId());
                cm.setLineName(m.getLine().getName());
            }
            return cm;
        }).collect(Collectors.toList());
        manifest.setMachines(cachedMachines);

        // 3. Cached BOM Items
        List<BomItem> bomItems = bomItemRepository.findAll();
        List<EdgeOfflineCacheManifestDto.CachedBomItemDto> cachedBoms = bomItems.stream().map(b -> {
            EdgeOfflineCacheManifestDto.CachedBomItemDto cb = new EdgeOfflineCacheManifestDto.CachedBomItemDto();
            cb.setId(b.getId());
            cb.setProductCode(b.getProductCode());
            cb.setComponentMaterialCode(b.getMaterialCode());
            cb.setComponentDescription(b.getMaterialName());
            cb.setQuantityRequired(b.getRequiredQuantityPerUnit() != null ? b.getRequiredQuantityPerUnit().doubleValue() : 1.0);
            cb.setUom(b.getUom());
            return cb;
        }).collect(Collectors.toList());
        manifest.setBomItems(cachedBoms);

        // 4. Cached Material Lots
        List<MaterialLot> lots = materialLotRepository.findAll();
        List<EdgeOfflineCacheManifestDto.CachedMaterialLotDto> cachedLots = lots.stream().map(l -> {
            EdgeOfflineCacheManifestDto.CachedMaterialLotDto cl = new EdgeOfflineCacheManifestDto.CachedMaterialLotDto();
            cl.setId(l.getId());
            cl.setLotNumber(l.getLotNumber());
            cl.setMaterialCode(l.getMaterialCode());
            cl.setMaterialName(l.getMaterialName());
            cl.setQuantityRemaining(l.getQuantity() != null ? l.getQuantity().doubleValue() : 0.0);
            cl.setUom(l.getUom());
            cl.setStatus(l.getStatus() != null ? l.getStatus() : "AVAILABLE");
            return cl;
        }).collect(Collectors.toList());
        manifest.setMaterialLots(cachedLots);

        log.info("Generated offline cache manifest for gateway {} (plant={}): {} orders, {} machines, {} BOMs, {} lots",
                gatewayCode, gateway.getPlant().getName(), cachedOrders.size(), cachedMachines.size(), cachedBoms.size(), cachedLots.size());

        return manifest;
    }

    public EdgeGatewayDto toDto(EdgeGateway gateway) {
        if (gateway == null) return null;
        EdgeGatewayDto dto = new EdgeGatewayDto();
        dto.setId(gateway.getId());
        dto.setGatewayCode(gateway.getGatewayCode());
        dto.setName(gateway.getName());
        if (gateway.getPlant() != null) {
            dto.setPlantId(gateway.getPlant().getId());
            dto.setPlantName(gateway.getPlant().getName());
        }
        dto.setIpAddress(gateway.getIpAddress());
        dto.setMacAddress(gateway.getMacAddress());
        dto.setStatus(gateway.getStatus());
        dto.setLastHeartbeatAt(gateway.getLastHeartbeatAt());
        dto.setLastSyncAt(gateway.getLastSyncAt());
        dto.setLastSyncSequenceId(gateway.getLastSyncSequenceId());
        dto.setBufferCapacityRecords(gateway.getBufferCapacityRecords());
        dto.setFirmwareVersion(gateway.getFirmwareVersion());
        dto.setCreatedAt(gateway.getCreatedAt());
        dto.setUpdatedAt(gateway.getUpdatedAt());
        return dto;
    }
}
