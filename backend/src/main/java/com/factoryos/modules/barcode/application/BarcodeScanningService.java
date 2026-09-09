package com.factoryos.modules.barcode.application;

import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.barcode.domain.*;
import com.factoryos.modules.barcode.dto.*;
import com.factoryos.modules.barcode.repository.BarcodeScanLogRepository;
import com.factoryos.modules.barcode.repository.BomItemRepository;
import com.factoryos.modules.barcode.repository.MaterialLotRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BarcodeScanningService {

    private static final Logger log = LoggerFactory.getLogger(BarcodeScanningService.class);

    private final MaterialLotRepository materialLotRepository;
    private final BomItemRepository bomItemRepository;
    private final BarcodeScanLogRepository barcodeScanLogRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final MachineRepository machineRepository;
    private final UserRepository userRepository;

    public BarcodeScanningService(
            MaterialLotRepository materialLotRepository,
            BomItemRepository bomItemRepository,
            BarcodeScanLogRepository barcodeScanLogRepository,
            ProductionOrderRepository productionOrderRepository,
            MachineRepository machineRepository,
            UserRepository userRepository) {
        this.materialLotRepository = materialLotRepository;
        this.bomItemRepository = bomItemRepository;
        this.barcodeScanLogRepository = barcodeScanLogRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.machineRepository = machineRepository;
        this.userRepository = userRepository;
    }

    public BarcodeScanResponse processScan(BarcodeScanRequest request, UUID userId, String userName) {
        long startTime = System.currentTimeMillis();
        String raw = request.getRawPayload() != null ? request.getRawPayload().trim() : "";
        String format = request.getBarcodeFormat() != null ? request.getBarcodeFormat() : "UNKNOWN";
        String source = request.getScannerSource() != null ? request.getScannerSource() : "HARDWARE_WEDGE";

        BarcodeScanLog scanLog = new BarcodeScanLog();
        scanLog.setScanPayload(raw);
        scanLog.setBarcodeFormat(format);
        scanLog.setScannerSource(source);
        scanLog.setMachineId(request.getMachineId());
        scanLog.setProductionOrderId(request.getProductionOrderId());
        scanLog.setScannedByUserId(userId);
        scanLog.setScannedByName(userName);

        try {
            // 1. Classify Payload & Extract Identifier
            BarcodeType detectedType = detectBarcodeType(raw);
            scanLog.setBarcodeType(detectedType);

            BarcodeScanResponse response;
            switch (detectedType) {
                case TRAVELER -> response = processTravelerScan(raw, request, scanLog);
                case MATERIAL_LOT -> response = processMaterialLotScan(raw, request, scanLog);
                case MACHINE_ASSET -> response = processMachineScan(raw, request, scanLog);
                case OPERATOR_BADGE -> response = processOperatorBadgeScan(raw, request, scanLog);
                default -> response = processGenericOrFallbackScan(raw, request, scanLog);
            }

            long latency = Math.max(1, System.currentTimeMillis() - startTime);
            scanLog.setLatencyMs(latency);
            response.setExecutionLatencyMs(latency);

            BarcodeScanLog saved = barcodeScanLogRepository.save(scanLog);
            response.setScanLogId(saved.getId());

            log.info("[BARCODE] Processed {} scan '{}' in {}ms - Result: {}", 
                    detectedType, raw, latency, response.getValidationStatus());
            return response;

        } catch (Exception e) {
            long latency = Math.max(1, System.currentTimeMillis() - startTime);
            scanLog.setLatencyMs(latency);
            scanLog.setValidationStatus(BarcodeValidationStatus.ERROR);
            scanLog.setErrorMessage(e.getMessage());
            BarcodeScanLog saved = barcodeScanLogRepository.save(scanLog);

            log.error("[BARCODE] Error processing scan '{}': {}", raw, e.getMessage(), e);
            return BarcodeScanResponse.invalid(
                    saved.getId(), raw, BarcodeType.UNKNOWN, BarcodeValidationStatus.ERROR, 
                    ResolvedEntityType.NONE, null, null, "Scan error: " + e.getMessage(), latency);
        }
    }

    private BarcodeType detectBarcodeType(String raw) {
        String upper = raw.toUpperCase();
        if (upper.startsWith("ORD:") || upper.startsWith("TRAVELER:") || upper.startsWith("PO-") || upper.startsWith("ORD-")) {
            return BarcodeType.TRAVELER;
        }
        if (upper.startsWith("LOT:") || upper.startsWith("RAW:") || upper.startsWith("MAT:") || upper.startsWith("LOT-")) {
            return BarcodeType.MATERIAL_LOT;
        }
        if (upper.startsWith("MACH:") || upper.startsWith("SN:") || upper.startsWith("ASSET:") || upper.startsWith("CNC-") || upper.startsWith("MILL-") || upper.startsWith("LATHE-")) {
            return BarcodeType.MACHINE_ASSET;
        }
        if (upper.startsWith("OPR:") || upper.startsWith("BADGE:") || upper.startsWith("USER:") || upper.startsWith("EMP-")) {
            return BarcodeType.OPERATOR_BADGE;
        }
        return BarcodeType.UNKNOWN;
    }

    private String extractCleanIdentifier(String raw) {
        if (raw.contains(":")) {
            return raw.substring(raw.indexOf(":") + 1).trim();
        }
        return raw.trim();
    }

    private BarcodeScanResponse processTravelerScan(String raw, BarcodeScanRequest request, BarcodeScanLog log) {
        String identifier = extractCleanIdentifier(raw);
        Optional<ProductionOrder> orderOpt = productionOrderRepository.findByOrderNumberAndIsDeletedFalse(identifier);
        
        if (orderOpt.isEmpty()) {
            orderOpt = productionOrderRepository.findByOrderNumber(identifier);
        }
        if (orderOpt.isEmpty()) {
            try {
                UUID orderId = UUID.fromString(identifier);
                orderOpt = productionOrderRepository.findByIdAndIsDeletedFalse(orderId);
            } catch (Exception ignored) {}
        }

        if (orderOpt.isEmpty()) {
            log.setValidationStatus(BarcodeValidationStatus.NOT_FOUND);
            log.setResolvedEntityType(ResolvedEntityType.PRODUCTION_ORDER);
            log.setErrorMessage("Production order traveler not found: " + identifier);
            return BarcodeScanResponse.invalid(
                    null, raw, BarcodeType.TRAVELER, BarcodeValidationStatus.NOT_FOUND,
                    ResolvedEntityType.PRODUCTION_ORDER, identifier, null,
                    "Production order traveler '" + identifier + "' not found in system.", 0);
        }

        ProductionOrder order = orderOpt.get();
        log.setValidationStatus(BarcodeValidationStatus.VALID);
        log.setResolvedEntityType(ResolvedEntityType.PRODUCTION_ORDER);
        log.setResolvedEntityId(order.getId().toString());
        log.setProductionOrderId(order.getId());
        if (order.getMachine() != null) {
            log.setMachineId(order.getMachine().getId());
        }

        String summary = String.format("Order: %s | Product: %s | Planned: %d | Status: %s | Machine: %s",
                order.getOrderNumber(), order.getProductCode(), order.getPlannedQuantity(), 
                order.getStatus(), order.getMachine() != null ? order.getMachine().getName() : "Unassigned");
        log.setResolvedEntitySummary(summary);
        log.setBomMatched(true);

        Map<String, Object> data = new HashMap<>();
        data.put("id", order.getId());
        data.put("orderNumber", order.getOrderNumber());
        data.put("productCode", order.getProductCode());
        data.put("productDescription", order.getProductDescription());
        data.put("plannedQuantity", order.getPlannedQuantity());
        data.put("goodQuantity", order.getGoodQuantity());
        data.put("scrapQuantity", order.getScrapQuantity());
        data.put("status", order.getStatus().name());
        data.put("machineId", order.getMachine() != null ? order.getMachine().getId() : null);
        data.put("machineName", order.getMachine() != null ? order.getMachine().getName() : null);

        return BarcodeScanResponse.valid(
                null, raw, BarcodeType.TRAVELER, ResolvedEntityType.PRODUCTION_ORDER,
                order.getId().toString(), summary, true, "Valid production order traveler verified.", 0, data);
    }

    private BarcodeScanResponse processMaterialLotScan(String raw, BarcodeScanRequest request, BarcodeScanLog log) {
        String identifier = extractCleanIdentifier(raw);
        Optional<MaterialLot> lotOpt = materialLotRepository.findByLotNumberIgnoreCase(identifier);

        if (lotOpt.isEmpty()) {
            log.setValidationStatus(BarcodeValidationStatus.NOT_FOUND);
            log.setResolvedEntityType(ResolvedEntityType.MATERIAL_LOT);
            log.setErrorMessage("Material lot not found: " + identifier);
            return BarcodeScanResponse.invalid(
                    null, raw, BarcodeType.MATERIAL_LOT, BarcodeValidationStatus.NOT_FOUND,
                    ResolvedEntityType.MATERIAL_LOT, identifier, null,
                    "Material lot '" + identifier + "' not found in inventory registry.", 0);
        }

        MaterialLot lot = lotOpt.get();
        log.setResolvedEntityType(ResolvedEntityType.MATERIAL_LOT);
        log.setResolvedEntityId(lot.getId().toString());

        // Check expiry and quarantine
        if ("EXPIRED".equalsIgnoreCase(lot.getStatus()) || lot.isExpired()) {
            log.setValidationStatus(BarcodeValidationStatus.EXPIRED);
            log.setBomMatched(false);
            log.setErrorMessage("Material lot is expired since " + lot.getExpiryDate());
            String summary = String.format("EXPIRED Lot: %s | Material: %s (%s) | Expiry: %s",
                    lot.getLotNumber(), lot.getMaterialCode(), lot.getMaterialName(), lot.getExpiryDate());
            log.setResolvedEntitySummary(summary);

            return BarcodeScanResponse.invalid(
                    null, raw, BarcodeType.MATERIAL_LOT, BarcodeValidationStatus.EXPIRED,
                    ResolvedEntityType.MATERIAL_LOT, lot.getId().toString(), summary,
                    "Material lot " + lot.getLotNumber() + " is EXPIRED. Do not load onto machine.", 0);
        }

        if ("QUARANTINED".equalsIgnoreCase(lot.getStatus())) {
            log.setValidationStatus(BarcodeValidationStatus.QUARANTINED);
            log.setBomMatched(false);
            log.setErrorMessage("Material lot is currently quarantined for quality inspection.");
            String summary = String.format("QUARANTINED Lot: %s | Material: %s", lot.getLotNumber(), lot.getMaterialCode());
            log.setResolvedEntitySummary(summary);

            return BarcodeScanResponse.invalid(
                    null, raw, BarcodeType.MATERIAL_LOT, BarcodeValidationStatus.QUARANTINED,
                    ResolvedEntityType.MATERIAL_LOT, lot.getId().toString(), summary,
                    "Material lot " + lot.getLotNumber() + " is QUARANTINED. Quality clearance required.", 0);
        }

        // BOM Cross-Verification against Active Order / Machine
        String targetProductCode = null;
        if (request.getProductionOrderId() != null) {
            Optional<ProductionOrder> po = productionOrderRepository.findByIdAndIsDeletedFalse(request.getProductionOrderId());
            if (po.isPresent()) targetProductCode = po.get().getProductCode();
        } else if (request.getMachineId() != null) {
            List<ProductionOrder> activeOrders = productionOrderRepository.findByMachineIdAndStatusAndIsDeletedFalse(
                    request.getMachineId(), ProductionOrderStatus.IN_PROGRESS);
            if (!activeOrders.isEmpty()) {
                targetProductCode = activeOrders.get(0).getProductCode();
                log.setProductionOrderId(activeOrders.get(0).getId());
            }
        }

        boolean bomMatched = true;
        String bomMessage = "Valid material lot verified.";

        if (targetProductCode != null) {
            List<BomItem> bomItems = bomItemRepository.findByProductCodeIgnoreCase(targetProductCode);
            boolean matched = bomItems.stream().anyMatch(b -> b.getMaterialCode().equalsIgnoreCase(lot.getMaterialCode()));

            if (!matched && !bomItems.isEmpty()) {
                bomMatched = false;
                log.setBomMatched(false);
                log.setValidationStatus(BarcodeValidationStatus.INVALID_BOM);
                String msg = String.format("Material '%s' (%s) is NOT part of BOM recipe for active product '%s'.",
                        lot.getMaterialCode(), lot.getMaterialName(), targetProductCode);
                log.setErrorMessage(msg);
                String summary = String.format("BOM MISMATCH: Lot %s | Mat: %s | Target: %s",
                        lot.getLotNumber(), lot.getMaterialCode(), targetProductCode);
                log.setResolvedEntitySummary(summary);

                return BarcodeScanResponse.invalid(
                        null, raw, BarcodeType.MATERIAL_LOT, BarcodeValidationStatus.INVALID_BOM,
                        ResolvedEntityType.MATERIAL_LOT, lot.getId().toString(), summary, msg, 0);
            } else if (matched) {
                bomMessage = String.format("Material '%s' MATCHED Bill of Materials recipe for active product '%s'.",
                        lot.getMaterialCode(), targetProductCode);
            }
        }

        log.setValidationStatus(BarcodeValidationStatus.VALID);
        log.setBomMatched(bomMatched);
        String summary = String.format("Lot: %s | Mat: %s (%s) | Qty: %s %s | Status: %s",
                lot.getLotNumber(), lot.getMaterialCode(), lot.getMaterialName(), lot.getQuantity(), lot.getUom(), lot.getStatus());
        log.setResolvedEntitySummary(summary);

        Map<String, Object> data = new HashMap<>();
        data.put("id", lot.getId());
        data.put("lotNumber", lot.getLotNumber());
        data.put("materialCode", lot.getMaterialCode());
        data.put("materialName", lot.getMaterialName());
        data.put("quantity", lot.getQuantity());
        data.put("uom", lot.getUom());
        data.put("status", lot.getStatus());
        data.put("expiryDate", lot.getExpiryDate());
        data.put("supplierName", lot.getSupplierName());
        data.put("bomMatched", bomMatched);

        return BarcodeScanResponse.valid(
                null, raw, BarcodeType.MATERIAL_LOT, ResolvedEntityType.MATERIAL_LOT,
                lot.getId().toString(), summary, bomMatched, bomMessage, 0, data);
    }

    private BarcodeScanResponse processMachineScan(String raw, BarcodeScanRequest request, BarcodeScanLog log) {
        String identifier = extractCleanIdentifier(raw);
        Optional<Machine> machineOpt = machineRepository.findBySerialNumberIgnoreCaseAndIsDeletedFalse(identifier);

        if (machineOpt.isEmpty()) {
            machineOpt = machineRepository.findByNameIgnoreCaseAndIsDeletedFalse(identifier);
        }
        if (machineOpt.isEmpty()) {
            try {
                UUID machineId = UUID.fromString(identifier);
                machineOpt = machineRepository.findByIdAndIsDeletedFalse(machineId);
            } catch (Exception ignored) {}
        }

        if (machineOpt.isEmpty()) {
            log.setValidationStatus(BarcodeValidationStatus.NOT_FOUND);
            log.setResolvedEntityType(ResolvedEntityType.MACHINE);
            log.setErrorMessage("Machine asset not found: " + identifier);
            return BarcodeScanResponse.invalid(
                    null, raw, BarcodeType.MACHINE_ASSET, BarcodeValidationStatus.NOT_FOUND,
                    ResolvedEntityType.MACHINE, identifier, null,
                    "Machine asset barcode '" + identifier + "' not recognized.", 0);
        }

        Machine machine = machineOpt.get();
        log.setValidationStatus(BarcodeValidationStatus.VALID);
        log.setResolvedEntityType(ResolvedEntityType.MACHINE);
        log.setResolvedEntityId(machine.getId().toString());
        log.setMachineId(machine.getId());
        log.setBomMatched(true);

        String summary = String.format("Machine: %s | Serial: %s | Status: %s | Loc: %s",
                machine.getName(), machine.getSerialNumber(), machine.getStatus(), machine.getLocation());
        log.setResolvedEntitySummary(summary);

        Map<String, Object> data = new HashMap<>();
        data.put("id", machine.getId());
        data.put("name", machine.getName());
        data.put("serialNumber", machine.getSerialNumber());
        data.put("status", machine.getStatus().name());
        data.put("location", machine.getLocation());

        return BarcodeScanResponse.valid(
                null, raw, BarcodeType.MACHINE_ASSET, ResolvedEntityType.MACHINE,
                machine.getId().toString(), summary, true, "Machine asset barcode verified.", 0, data);
    }

    private BarcodeScanResponse processOperatorBadgeScan(String raw, BarcodeScanRequest request, BarcodeScanLog log) {
        String identifier = extractCleanIdentifier(raw);
        Optional<User> userOpt = userRepository.findByEmailIgnoreCaseAndIsDeletedFalse(identifier);

        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByDisplayNameIgnoreCaseAndIsDeletedFalse(identifier);
        }
        if (userOpt.isEmpty()) {
            try {
                UUID userId = UUID.fromString(identifier);
                userOpt = userRepository.findByIdAndIsDeletedFalse(userId);
            } catch (Exception ignored) {}
        }

        if (userOpt.isEmpty()) {
            log.setValidationStatus(BarcodeValidationStatus.NOT_FOUND);
            log.setResolvedEntityType(ResolvedEntityType.USER);
            log.setErrorMessage("Operator badge not found: " + identifier);
            return BarcodeScanResponse.invalid(
                    null, raw, BarcodeType.OPERATOR_BADGE, BarcodeValidationStatus.NOT_FOUND,
                    ResolvedEntityType.USER, identifier, null,
                    "Operator badge '" + identifier + "' not registered in access directory.", 0);
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            log.setValidationStatus(BarcodeValidationStatus.UNAUTHORIZED);
            log.setResolvedEntityType(ResolvedEntityType.USER);
            log.setResolvedEntityId(user.getId().toString());
            log.setErrorMessage("Operator account is deactivated.");
            return BarcodeScanResponse.invalid(
                    null, raw, BarcodeType.OPERATOR_BADGE, BarcodeValidationStatus.UNAUTHORIZED,
                    ResolvedEntityType.USER, user.getId().toString(), user.getDisplayName() + " (INACTIVE)",
                    "Operator badge belongs to a deactivated account.", 0);
        }

        log.setValidationStatus(BarcodeValidationStatus.VALID);
        log.setResolvedEntityType(ResolvedEntityType.USER);
        log.setResolvedEntityId(user.getId().toString());
        log.setBomMatched(true);

        String roleName = (user.getRole() != null && user.getRole().getName() != null) 
                ? user.getRole().getName().name() 
                : "OPERATOR";

        String summary = String.format("Operator: %s | Email: %s | Role: %s",
                user.getDisplayName(), user.getEmail(), roleName);
        log.setResolvedEntitySummary(summary);

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("displayName", user.getDisplayName());
        data.put("email", user.getEmail());
        data.put("role", roleName);

        return BarcodeScanResponse.valid(
                null, raw, BarcodeType.OPERATOR_BADGE, ResolvedEntityType.USER,
                user.getId().toString(), summary, true, "Operator badge verified.", 0, data);
    }

    private BarcodeScanResponse processGenericOrFallbackScan(String raw, BarcodeScanRequest request, BarcodeScanLog log) {
        // Attempt fallback lookup across all entity repositories
        // 1. Check MaterialLot
        Optional<MaterialLot> lotOpt = materialLotRepository.findByLotNumberIgnoreCase(raw);
        if (lotOpt.isPresent()) {
            log.setBarcodeType(BarcodeType.MATERIAL_LOT);
            return processMaterialLotScan(raw, request, log);
        }

        // 2. Check ProductionOrder
        Optional<ProductionOrder> orderOpt = productionOrderRepository.findByOrderNumberAndIsDeletedFalse(raw);
        if (orderOpt.isEmpty()) {
            orderOpt = productionOrderRepository.findByOrderNumber(raw);
        }
        if (orderOpt.isPresent()) {
            log.setBarcodeType(BarcodeType.TRAVELER);
            return processTravelerScan(raw, request, log);
        }

        // 3. Check Machine
        Optional<Machine> machineOpt = machineRepository.findBySerialNumberIgnoreCaseAndIsDeletedFalse(raw);
        if (machineOpt.isEmpty()) {
            machineOpt = machineRepository.findByNameIgnoreCaseAndIsDeletedFalse(raw);
        }
        if (machineOpt.isPresent()) {
            log.setBarcodeType(BarcodeType.MACHINE_ASSET);
            return processMachineScan(raw, request, log);
        }

        log.setValidationStatus(BarcodeValidationStatus.NOT_FOUND);
        log.setResolvedEntityType(ResolvedEntityType.NONE);
        log.setErrorMessage("Barcode payload could not be matched to any order, material lot, machine, or operator.");

        return BarcodeScanResponse.invalid(
                null, raw, BarcodeType.UNKNOWN, BarcodeValidationStatus.NOT_FOUND,
                ResolvedEntityType.NONE, null, null,
                "Unknown barcode format or unregistered payload: '" + raw + "'", 0);
    }

    @Transactional(readOnly = true)
    public List<BarcodeScanLogDto> getRecentScanLogs() {
        return barcodeScanLogRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(this::mapToScanLogDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BomItemDto> getBomForProduct(String productCode) {
        return bomItemRepository.findByProductCodeIgnoreCase(productCode).stream()
                .map(b -> new BomItemDto(b.getId(), b.getProductCode(), b.getMaterialCode(), b.getMaterialName(), b.getRequiredQuantityPerUnit(), b.getUom()))
                .collect(Collectors.toList());
    }

    private BarcodeScanLogDto mapToScanLogDto(BarcodeScanLog log) {
        BarcodeScanLogDto dto = new BarcodeScanLogDto();
        dto.setId(log.getId());
        dto.setScanPayload(log.getScanPayload());
        dto.setBarcodeFormat(log.getBarcodeFormat());
        dto.setBarcodeType(log.getBarcodeType());
        dto.setScannerSource(log.getScannerSource());
        dto.setResolvedEntityType(log.getResolvedEntityType());
        dto.setResolvedEntityId(log.getResolvedEntityId());
        dto.setResolvedEntitySummary(log.getResolvedEntitySummary());
        dto.setMachineId(log.getMachineId());
        dto.setProductionOrderId(log.getProductionOrderId());
        dto.setValidationStatus(log.getValidationStatus());
        dto.setBomMatched(log.isBomMatched());
        dto.setErrorMessage(log.getErrorMessage());
        dto.setScannedByUserId(log.getScannedByUserId());
        dto.setScannedByName(log.getScannedByName());
        dto.setLatencyMs(log.getLatencyMs());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
