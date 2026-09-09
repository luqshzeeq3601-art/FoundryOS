package com.factoryos.modules.barcode.application;

import com.factoryos.modules.auth.domain.Role;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.barcode.domain.*;
import com.factoryos.modules.barcode.dto.BarcodeScanRequest;
import com.factoryos.modules.barcode.dto.BarcodeScanResponse;
import com.factoryos.modules.barcode.repository.BarcodeScanLogRepository;
import com.factoryos.modules.barcode.repository.BomItemRepository;
import com.factoryos.modules.barcode.repository.MaterialLotRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BarcodeScanningServiceTest {

    @Mock
    private MaterialLotRepository materialLotRepository;

    @Mock
    private BomItemRepository bomItemRepository;

    @Mock
    private BarcodeScanLogRepository barcodeScanLogRepository;

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private UserRepository userRepository;

    private BarcodeScanningService barcodeScanningService;

    private UUID machineId;
    private Machine testMachine;
    private ProductionOrder testOrder;

    @BeforeEach
    void setUp() {
        barcodeScanningService = new BarcodeScanningService(
                materialLotRepository,
                bomItemRepository,
                barcodeScanLogRepository,
                productionOrderRepository,
                machineRepository,
                userRepository
        );

        machineId = UUID.randomUUID();
        testMachine = new Machine();
        testMachine.setId(machineId);
        testMachine.setSerialNumber("CNC-01");
        testMachine.setName("CNC Milling Center #1");
        testMachine.setLocation("BAY-1");
        testMachine.setStatus(MachineStatus.RUNNING);

        testOrder = new ProductionOrder();
        testOrder.setId(UUID.randomUUID());
        testOrder.setOrderNumber("ORD-2026-001");
        testOrder.setMachine(testMachine);
        testOrder.setProductCode("BRACKET-01");
        testOrder.setPlannedQuantity(500);
        testOrder.setStatus(ProductionOrderStatus.IN_PROGRESS);

        when(barcodeScanLogRepository.save(any(BarcodeScanLog.class))).thenAnswer(inv -> {
            BarcodeScanLog log = inv.getArgument(0);
            if (log.getId() == null) {
                log.setId(UUID.randomUUID());
            }
            return log;
        });
    }

    @Test
    void testProcessTravelerScan_ValidOrder() {
        when(productionOrderRepository.findByOrderNumberAndIsDeletedFalse("ORD-2026-001")).thenReturn(Optional.of(testOrder));

        BarcodeScanRequest request = new BarcodeScanRequest("ORD:ORD-2026-001", "QR_CODE", "HARDWARE_WEDGE", machineId, null);
        BarcodeScanResponse response = barcodeScanningService.processScan(request, UUID.randomUUID(), "Operator 1");

        assertNotNull(response);
        assertEquals(BarcodeValidationStatus.VALID, response.getValidationStatus());
        assertEquals(BarcodeType.TRAVELER, response.getBarcodeType());
        assertEquals(ResolvedEntityType.PRODUCTION_ORDER, response.getResolvedEntityType());
        assertEquals(testOrder.getId().toString(), response.getResolvedEntityId());
        assertTrue(response.isBomMatched());
        verify(barcodeScanLogRepository).save(any(BarcodeScanLog.class));
    }

    @Test
    void testProcessTravelerScan_NotFound() {
        when(productionOrderRepository.findByOrderNumberAndIsDeletedFalse("UNKNOWN-999")).thenReturn(Optional.empty());

        BarcodeScanRequest request = new BarcodeScanRequest("ORD:UNKNOWN-999", "CODE_128", "CAMERA_ZXING", machineId, null);
        BarcodeScanResponse response = barcodeScanningService.processScan(request, UUID.randomUUID(), "Operator 1");

        assertNotNull(response);
        assertEquals(BarcodeValidationStatus.NOT_FOUND, response.getValidationStatus());
        assertEquals(BarcodeType.TRAVELER, response.getBarcodeType());
        assertFalse(response.isBomMatched());
    }

    @Test
    void testProcessMaterialLotScan_ValidBOMMatch() {
        MaterialLot lot = new MaterialLot("LOT-ALU-6061-001", "RAW-ALU-6061", "Aluminum 6061 Billet",
                BigDecimal.valueOf(500.0), "KG", "AVAILABLE", Instant.now().plus(90, ChronoUnit.DAYS), "Alcoa");
        lot.setId(UUID.randomUUID());

        when(materialLotRepository.findByLotNumberIgnoreCase("LOT-ALU-6061-001")).thenReturn(Optional.of(lot));
        when(productionOrderRepository.findByMachineIdAndStatusAndIsDeletedFalse(machineId, ProductionOrderStatus.IN_PROGRESS))
                .thenReturn(List.of(testOrder));

        BomItem bomItem = new BomItem("BRACKET-01", "RAW-ALU-6061", "Aluminum 6061 Billet", BigDecimal.valueOf(1.25), "KG");
        when(bomItemRepository.findByProductCodeIgnoreCase("BRACKET-01")).thenReturn(List.of(bomItem));

        BarcodeScanRequest request = new BarcodeScanRequest("LOT:LOT-ALU-6061-001", "DATA_MATRIX", "HARDWARE_WEDGE", machineId, null);
        BarcodeScanResponse response = barcodeScanningService.processScan(request, UUID.randomUUID(), "Operator 1");

        assertNotNull(response);
        assertEquals(BarcodeValidationStatus.VALID, response.getValidationStatus());
        assertEquals(BarcodeType.MATERIAL_LOT, response.getBarcodeType());
        assertTrue(response.isBomMatched());
        assertTrue(response.getMessage().contains("MATCHED Bill of Materials"));
    }

    @Test
    void testProcessMaterialLotScan_InvalidBOMMismatch() {
        MaterialLot lot = new MaterialLot("LOT-STL-4140-002", "RAW-STL-4140", "Steel Bar 4140",
                BigDecimal.valueOf(300.0), "KG", "AVAILABLE", Instant.now().plus(180, ChronoUnit.DAYS), "Timken");
        lot.setId(UUID.randomUUID());

        when(materialLotRepository.findByLotNumberIgnoreCase("LOT-STL-4140-002")).thenReturn(Optional.of(lot));
        when(productionOrderRepository.findByMachineIdAndStatusAndIsDeletedFalse(machineId, ProductionOrderStatus.IN_PROGRESS))
                .thenReturn(List.of(testOrder));

        BomItem bomItem = new BomItem("BRACKET-01", "RAW-ALU-6061", "Aluminum 6061 Billet", BigDecimal.valueOf(1.25), "KG");
        when(bomItemRepository.findByProductCodeIgnoreCase("BRACKET-01")).thenReturn(List.of(bomItem));

        BarcodeScanRequest request = new BarcodeScanRequest("LOT:LOT-STL-4140-002", "DATA_MATRIX", "HARDWARE_WEDGE", machineId, null);
        BarcodeScanResponse response = barcodeScanningService.processScan(request, UUID.randomUUID(), "Operator 1");

        assertNotNull(response);
        assertEquals(BarcodeValidationStatus.INVALID_BOM, response.getValidationStatus());
        assertFalse(response.isBomMatched());
        assertTrue(response.getMessage().contains("NOT part of BOM recipe"));
    }

    @Test
    void testProcessMaterialLotScan_ExpiredLot() {
        MaterialLot expiredLot = new MaterialLot("LOT-EXP-001", "RAW-EPOXY", "Epoxy",
                BigDecimal.valueOf(10.0), "L", "EXPIRED", Instant.now().minus(5, ChronoUnit.DAYS), "Henkel");
        expiredLot.setId(UUID.randomUUID());

        when(materialLotRepository.findByLotNumberIgnoreCase("LOT-EXP-001")).thenReturn(Optional.of(expiredLot));

        BarcodeScanRequest request = new BarcodeScanRequest("LOT:LOT-EXP-001", "QR_CODE", "CAMERA_ZXING", machineId, null);
        BarcodeScanResponse response = barcodeScanningService.processScan(request, UUID.randomUUID(), "Operator 1");

        assertNotNull(response);
        assertEquals(BarcodeValidationStatus.EXPIRED, response.getValidationStatus());
        assertFalse(response.isBomMatched());
        assertTrue(response.getMessage().contains("EXPIRED"));
    }

    @Test
    void testProcessMachineScan_Valid() {
        when(machineRepository.findBySerialNumberIgnoreCaseAndIsDeletedFalse("CNC-01")).thenReturn(Optional.of(testMachine));

        BarcodeScanRequest request = new BarcodeScanRequest("MACH:CNC-01", "CODE_128", "HARDWARE_WEDGE", null, null);
        BarcodeScanResponse response = barcodeScanningService.processScan(request, UUID.randomUUID(), "Operator 1");

        assertNotNull(response);
        assertEquals(BarcodeValidationStatus.VALID, response.getValidationStatus());
        assertEquals(BarcodeType.MACHINE_ASSET, response.getBarcodeType());
        assertEquals(ResolvedEntityType.MACHINE, response.getResolvedEntityType());
    }

    @Test
    void testProcessOperatorBadgeScan_Valid() {
        Role opRole = new Role();
        opRole.setName(RoleType.OPERATOR);

        User operator = new User();
        operator.setId(UUID.randomUUID());
        operator.setEmail("operator@factoryos.local");
        operator.setDisplayName("John Operator");
        operator.setRole(opRole);
        operator.setActive(true);

        when(userRepository.findByEmailIgnoreCaseAndIsDeletedFalse("operator@factoryos.local")).thenReturn(Optional.of(operator));

        BarcodeScanRequest request = new BarcodeScanRequest("OPR:operator@factoryos.local", "QR_CODE", "HARDWARE_WEDGE", null, null);
        BarcodeScanResponse response = barcodeScanningService.processScan(request, UUID.randomUUID(), "Admin");

        assertNotNull(response);
        assertEquals(BarcodeValidationStatus.VALID, response.getValidationStatus());
        assertEquals(BarcodeType.OPERATOR_BADGE, response.getBarcodeType());
        assertEquals(ResolvedEntityType.USER, response.getResolvedEntityType());
    }
}
