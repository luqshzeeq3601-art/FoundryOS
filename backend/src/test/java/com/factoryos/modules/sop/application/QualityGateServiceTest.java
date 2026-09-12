package com.factoryos.modules.sop.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.Role;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import com.factoryos.modules.sop.domain.*;
import com.factoryos.modules.sop.dto.QualityGateStatusDto;
import com.factoryos.modules.sop.dto.QualitySignOffRequestDto;
import com.factoryos.modules.sop.repository.QualitySignOffGateRepository;
import com.factoryos.modules.sop.repository.SopExecutionSessionRepository;
import com.factoryos.modules.sop.repository.StandardOperatingProcedureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QualityGateServiceTest {

    @Mock
    private QualitySignOffGateRepository qualityGateRepository;

    @Mock
    private SopExecutionSessionRepository sessionRepository;

    @Mock
    private StandardOperatingProcedureRepository sopRepository;

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private AuditRecordingService auditRecordingService;

    private QualityGateService qualityGateService;

    @BeforeEach
    void setUp() {
        qualityGateService = new QualityGateService(
                qualityGateRepository,
                sessionRepository,
                sopRepository,
                productionOrderRepository,
                auditRecordingService
        );
    }

    @Test
    void getGateStatusForOrder_NoSopConfigured_ReturnsCompliant() {
        UUID orderId = UUID.randomUUID();
        ProductionOrder order = new ProductionOrder();
        order.setId(orderId);
        order.setOrderNumber("PO-2026-001");
        order.setProductCode("CUSTOM-SKU-999");

        when(productionOrderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(qualityGateRepository.findByProductionOrderId(orderId)).thenReturn(Optional.empty());
        when(sopRepository.findByProductCodeIgnoreCaseAndIsDeletedFalse("CUSTOM-SKU-999")).thenReturn(List.of());
        when(sopRepository.findByProductCodeAndIsDeletedFalse("*")).thenReturn(List.of());

        QualityGateStatusDto status = qualityGateService.getGateStatusForOrder(orderId);

        assertNotNull(status);
        assertEquals(GateStatus.PASSED, status.getGateStatus());
        assertTrue(status.isCompliant());
    }

    @Test
    void getGateStatusForOrder_SopExistsWithNoSession_ReturnsPending() {
        UUID orderId = UUID.randomUUID();
        ProductionOrder order = new ProductionOrder();
        order.setId(orderId);
        order.setOrderNumber("PO-TURBINE-001");
        order.setProductCode("TURBINE-BLADE-V2");

        StandardOperatingProcedure sop = new StandardOperatingProcedure();
        sop.setId(UUID.randomUUID());
        sop.setSopCode("SOP-TURBINE-001");
        sop.setTitle("Turbine Blade Machining SOP");
        sop.setRequiresQualitySignOff(true);

        SopStep step1 = new SopStep();
        step1.setMandatory(true);
        sop.setSteps(List.of(step1));

        when(productionOrderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(qualityGateRepository.findByProductionOrderId(orderId)).thenReturn(Optional.empty());
        when(sopRepository.findByProductCodeIgnoreCaseAndIsDeletedFalse("TURBINE-BLADE-V2")).thenReturn(List.of(sop));

        QualityGateStatusDto status = qualityGateService.getGateStatusForOrder(orderId);

        assertNotNull(status);
        assertEquals(GateStatus.PENDING, status.getGateStatus());
        assertFalse(status.isCompliant());
        assertEquals(1, status.getTotalMandatorySteps());
        assertNotNull(status.getBlockingReason());
    }

    @Test
    void validateOrderCompletionGate_PendingGate_ThrowsAppException() {
        UUID orderId = UUID.randomUUID();
        ProductionOrder order = new ProductionOrder();
        order.setId(orderId);
        order.setOrderNumber("PO-TURBINE-001");
        order.setProductCode("TURBINE-BLADE-V2");

        StandardOperatingProcedure sop = new StandardOperatingProcedure();
        sop.setId(UUID.randomUUID());
        sop.setSopCode("SOP-TURBINE-001");
        sop.setRequiresQualitySignOff(true);
        sop.setSteps(List.of());

        when(productionOrderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(qualityGateRepository.findByProductionOrderId(orderId)).thenReturn(Optional.empty());
        when(sopRepository.findByProductCodeIgnoreCaseAndIsDeletedFalse("TURBINE-BLADE-V2")).thenReturn(List.of(sop));

        AppException ex = assertThrows(AppException.class, () ->
                qualityGateService.validateOrderCompletionGate(orderId));

        assertTrue(ex.getMessage().contains("QUALITY_GATE_FAILED"));
    }

    @Test
    void validateOrderCompletionGate_PassedGate_CompletesWithoutException() {
        UUID orderId = UUID.randomUUID();
        ProductionOrder order = new ProductionOrder();
        order.setId(orderId);
        order.setOrderNumber("PO-TURBINE-001");
        order.setProductCode("TURBINE-BLADE-V2");

        QualitySignOffGate gate = new QualitySignOffGate();
        gate.setId(UUID.randomUUID());
        gate.setProductionOrder(order);
        gate.setGateStatus(GateStatus.PASSED);

        when(productionOrderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(qualityGateRepository.findByProductionOrderId(orderId)).thenReturn(Optional.of(gate));

        assertDoesNotThrow(() -> qualityGateService.validateOrderCompletionGate(orderId));
    }

    @Test
    void signOffGate_RequiresQualityRole_RejectsOperator() {
        UUID orderId = UUID.randomUUID();
        ProductionOrder order = new ProductionOrder();
        order.setId(orderId);
        order.setOrderNumber("PO-TURBINE-001");
        order.setProductCode("TURBINE-BLADE-V2");

        StandardOperatingProcedure sop = new StandardOperatingProcedure();
        sop.setId(UUID.randomUUID());
        sop.setRequiresQualitySignOff(true);

        Role opRole = new Role();
        opRole.setName(RoleType.OPERATOR);
        User operator = new User();
        operator.setId(UUID.randomUUID());
        operator.setRole(opRole);

        when(productionOrderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(sopRepository.findByProductCodeIgnoreCaseAndIsDeletedFalse("TURBINE-BLADE-V2")).thenReturn(List.of(sop));

        QualitySignOffRequestDto req = new QualitySignOffRequestDto();
        req.setGateStatus(GateStatus.PASSED);
        req.setSignOffComments("Looks good");

        AppException ex = assertThrows(AppException.class, () ->
                qualityGateService.signOffGate(orderId, req, operator));

        assertTrue(ex.getMessage().contains("Quality Sign-Off requires an authorized Quality Inspector"));
    }

    @Test
    void signOffGate_EngineerRole_SignsOffSuccessfully() {
        UUID orderId = UUID.randomUUID();
        ProductionOrder order = new ProductionOrder();
        order.setId(orderId);
        order.setOrderNumber("PO-TURBINE-001");
        order.setProductCode("TURBINE-BLADE-V2");

        StandardOperatingProcedure sop = new StandardOperatingProcedure();
        sop.setId(UUID.randomUUID());
        sop.setRequiresQualitySignOff(true);
        sop.setSteps(List.of());

        Role engineerRole = new Role();
        engineerRole.setName(RoleType.ENGINEER);
        User engineer = new User();
        engineer.setId(UUID.randomUUID());
        engineer.setDisplayName("Alice Engineer");
        engineer.setRole(engineerRole);

        when(productionOrderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(sopRepository.findByProductCodeIgnoreCaseAndIsDeletedFalse("TURBINE-BLADE-V2")).thenReturn(List.of(sop));
        when(qualityGateRepository.findByProductionOrderId(orderId)).thenReturn(Optional.empty());
        when(sessionRepository.findFirstByProductionOrderIdOrderByStartedAtDesc(orderId)).thenReturn(Optional.empty());
        when(qualityGateRepository.save(any(QualitySignOffGate.class))).thenAnswer(i -> {
            QualitySignOffGate g = i.getArgument(0);
            g.setId(UUID.randomUUID());
            return g;
        });

        QualitySignOffRequestDto req = new QualitySignOffRequestDto();
        req.setGateStatus(GateStatus.PASSED);
        req.setSignOffComments("CMM inspection verified 100%. Zero tolerance drift.");

        QualityGateStatusDto result = qualityGateService.signOffGate(orderId, req, engineer);

        assertNotNull(result);
        assertEquals(GateStatus.PASSED, result.getGateStatus());
        assertTrue(result.isCompliant());
        assertEquals("Alice Engineer", result.getSignedOffByName());
        verify(auditRecordingService).record(eq(engineer.getId()), eq("QUALITY_GATE_SIGNED_OFF"), eq("QualitySignOffGate"), any(), isNull(), anyMap());
    }
}
