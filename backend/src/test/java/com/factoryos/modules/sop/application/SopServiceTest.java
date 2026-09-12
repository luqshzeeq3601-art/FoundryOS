package com.factoryos.modules.sop.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import com.factoryos.modules.sop.domain.*;
import com.factoryos.modules.sop.dto.*;
import com.factoryos.modules.sop.repository.*;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.repository.PlantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SopServiceTest {

    @Mock
    private StandardOperatingProcedureRepository sopRepository;

    @Mock
    private SopStepRepository sopStepRepository;

    @Mock
    private SopExecutionSessionRepository sessionRepository;

    @Mock
    private SopStepExecutionRecordRepository stepRecordRepository;

    @Mock
    private QualitySignOffGateRepository qualityGateRepository;

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private PlantRepository plantRepository;

    @Mock
    private AuditRecordingService auditRecordingService;

    private SopService sopService;

    @BeforeEach
    void setUp() {
        sopService = new SopService(
                sopRepository,
                sopStepRepository,
                sessionRepository,
                stepRecordRepository,
                qualityGateRepository,
                productionOrderRepository,
                machineRepository,
                plantRepository,
                auditRecordingService
        );
    }

    @Test
    void createSop_Success() {
        SopCreateRequestDto req = new SopCreateRequestDto();
        req.setSopCode("SOP-TURBINE-001");
        req.setTitle("Turbine Blade Machining SOP");
        req.setProductCode("TURBINE-BLADE-V2");
        req.setCategory(SopCategory.ASSEMBLY);
        req.setEstimatedDurationMinutes(45);
        req.setRequiresQualitySignOff(true);

        SopStepDto s1 = new SopStepDto();
        s1.setStepNumber(1);
        s1.setTitle("Verify Blade Alignment");
        s1.setStepType(SopStepType.NUMERIC_MEASUREMENT);
        s1.setNominalValue(12.50);
        s1.setMinTolerance(12.45);
        s1.setMaxTolerance(12.55);
        s1.setUnitOfMeasure("mm");
        s1.setMandatory(true);

        req.setSteps(List.of(s1));

        when(sopRepository.existsBySopCodeAndIsDeletedFalse("SOP-TURBINE-001")).thenReturn(false);
        when(sopRepository.save(any(StandardOperatingProcedure.class))).thenAnswer(i -> {
            StandardOperatingProcedure s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setDisplayName("John Doe");

        SopDto result = sopService.createSop(req, user);

        assertNotNull(result);
        assertEquals("SOP-TURBINE-001", result.getSopCode());
        assertEquals("Turbine Blade Machining SOP", result.getTitle());
        assertEquals(1, result.getSteps().size());
        assertTrue(result.isRequiresQualitySignOff());
    }

    @Test
    void startExecutionSession_Success_InitializesStepRecordsAndQualityGate() {
        UUID orderId = UUID.randomUUID();
        ProductionOrder order = new ProductionOrder();
        order.setId(orderId);
        order.setOrderNumber("PO-2026-001");
        order.setProductCode("TURBINE-BLADE-V2");

        Plant plant = new Plant();
        plant.setId(UUID.randomUUID());
        plant.setName("Austin Plant");
        order.setPlant(plant);

        StandardOperatingProcedure sop = new StandardOperatingProcedure();
        sop.setId(UUID.randomUUID());
        sop.setSopCode("SOP-TURBINE-001");
        sop.setTitle("Turbine Blade Machining");
        sop.setRequiresQualitySignOff(true);

        SopStep step1 = new SopStep();
        step1.setId(UUID.randomUUID());
        step1.setStepNumber(1);
        step1.setTitle("Alignment Check");
        step1.setMandatory(true);
        sop.setSteps(List.of(step1));

        when(productionOrderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(sopRepository.findByIdAndIsDeletedFalse(sop.getId())).thenReturn(Optional.of(sop));
        when(sessionRepository.save(any(SopExecutionSession.class))).thenAnswer(i -> {
            SopExecutionSession s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(qualityGateRepository.findByProductionOrderId(orderId)).thenReturn(Optional.empty());

        StartSopSessionRequestDto req = new StartSopSessionRequestDto();
        req.setProductionOrderId(orderId);
        req.setSopId(sop.getId());

        User operator = new User();
        operator.setId(UUID.randomUUID());
        operator.setDisplayName("Tech Bob");

        SopExecutionSessionDto sessionDto = sopService.startExecutionSession(req, operator);

        assertNotNull(sessionDto);
        assertEquals(SopSessionStatus.IN_PROGRESS, sessionDto.getSessionStatus());
        assertEquals("Tech Bob", sessionDto.getOperatorName());
        verify(stepRecordRepository).save(any(SopStepExecutionRecord.class));
        verify(qualityGateRepository).save(any(QualitySignOffGate.class));
    }

    @Test
    void recordStepExecution_NumericMeasurementWithinTolerance_SetsPassed() {
        UUID sessionId = UUID.randomUUID();
        UUID stepId = UUID.randomUUID();

        SopExecutionSession session = new SopExecutionSession();
        session.setId(sessionId);

        SopStep step = new SopStep();
        step.setId(stepId);
        step.setStepType(SopStepType.NUMERIC_MEASUREMENT);
        step.setNominalValue(15.00);
        step.setMinTolerance(14.80);
        step.setMaxTolerance(15.20);
        step.setMandatory(true);

        SopStepExecutionRecord record = new SopStepExecutionRecord();
        record.setId(UUID.randomUUID());
        record.setSession(session);
        record.setStep(step);
        record.setStatus(StepRecordStatus.PENDING);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(stepRecordRepository.findBySessionIdAndStepId(sessionId, stepId)).thenReturn(Optional.of(record));
        when(stepRecordRepository.save(any(SopStepExecutionRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(stepRecordRepository.findBySessionIdOrderByStepNumberAsc(sessionId)).thenReturn(List.of(record));

        RecordStepExecutionRequestDto req = new RecordStepExecutionRequestDto();
        req.setStepId(stepId);
        req.setNumericValue(15.05); // within 14.80 - 15.20
        req.setNotes("Laser micrometer measurement");

        User op = new User();
        op.setDisplayName("Tech Alice");

        SopStepExecutionRecordDto result = sopService.recordStepExecution(sessionId, req, op);

        assertNotNull(result);
        assertEquals(StepRecordStatus.PASSED, result.getStatus());
        assertTrue(result.getIsWithinTolerance());
        assertEquals(15.05, result.getNumericValue());
        assertEquals(SopSessionStatus.PASSED, session.getSessionStatus());
    }

    @Test
    void recordStepExecution_NumericMeasurementOutsideTolerance_SetsFailed() {
        UUID sessionId = UUID.randomUUID();
        UUID stepId = UUID.randomUUID();

        SopExecutionSession session = new SopExecutionSession();
        session.setId(sessionId);

        SopStep step = new SopStep();
        step.setId(stepId);
        step.setStepType(SopStepType.NUMERIC_MEASUREMENT);
        step.setNominalValue(15.00);
        step.setMinTolerance(14.80);
        step.setMaxTolerance(15.20);
        step.setMandatory(true);

        SopStepExecutionRecord record = new SopStepExecutionRecord();
        record.setId(UUID.randomUUID());
        record.setSession(session);
        record.setStep(step);
        record.setStatus(StepRecordStatus.PENDING);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(stepRecordRepository.findBySessionIdAndStepId(sessionId, stepId)).thenReturn(Optional.of(record));
        when(stepRecordRepository.save(any(SopStepExecutionRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(stepRecordRepository.findBySessionIdOrderByStepNumberAsc(sessionId)).thenReturn(List.of(record));

        RecordStepExecutionRequestDto req = new RecordStepExecutionRequestDto();
        req.setStepId(stepId);
        req.setNumericValue(15.45); // Out of tolerance (> 15.20)
        req.setNotes("Over tolerance limit!");

        User op = new User();
        op.setDisplayName("Tech Alice");

        SopStepExecutionRecordDto result = sopService.recordStepExecution(sessionId, req, op);

        assertNotNull(result);
        assertEquals(StepRecordStatus.FAILED, result.getStatus());
        assertFalse(result.getIsWithinTolerance());
        assertEquals(15.45, result.getNumericValue());
        assertEquals(SopSessionStatus.FAILED, session.getSessionStatus());
    }

    @Test
    void completeExecutionSession_WithIncompleteMandatorySteps_ThrowsAppException() {
        UUID sessionId = UUID.randomUUID();
        SopExecutionSession session = new SopExecutionSession();
        session.setId(sessionId);

        SopStep mandatoryStep = new SopStep();
        mandatoryStep.setMandatory(true);

        SopStepExecutionRecord pendingRecord = new SopStepExecutionRecord();
        pendingRecord.setStep(mandatoryStep);
        pendingRecord.setStatus(StepRecordStatus.PENDING);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(stepRecordRepository.findBySessionIdOrderByStepNumberAsc(sessionId)).thenReturn(List.of(pendingRecord));

        QualitySignOffRequestDto signOff = new QualitySignOffRequestDto();
        signOff.setGateStatus(GateStatus.PASSED);

        AppException ex = assertThrows(AppException.class, () ->
                sopService.completeExecutionSession(sessionId, signOff, null));

        assertTrue(ex.getMessage().contains("Not all mandatory inspection steps have been completed"));
    }
}
