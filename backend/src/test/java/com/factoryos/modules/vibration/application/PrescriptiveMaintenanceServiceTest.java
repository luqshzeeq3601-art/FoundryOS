package com.factoryos.modules.vibration.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.maintenance.domain.MaintenancePriority;
import com.factoryos.modules.maintenance.domain.MaintenanceStatus;
import com.factoryos.modules.maintenance.domain.MaintenanceWorkOrder;
import com.factoryos.modules.maintenance.dto.WorkOrderDto;
import com.factoryos.modules.maintenance.repository.MaintenanceWorkOrderRepository;
import com.factoryos.modules.vibration.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptiveMaintenanceServiceTest {

    @Mock
    private MaintenanceWorkOrderRepository workOrderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditRecordingService auditRecordingService;

    private ObjectMapper objectMapper;
    private PrescriptiveMaintenanceService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new PrescriptiveMaintenanceService(
                workOrderRepository,
                userRepository,
                auditRecordingService,
                objectMapper
        );
    }

    @Test
    void evaluateAndCreatePrescriptiveOrder_HealthyMachine_DoesNotCreateOrder() {
        Machine machine = new Machine();
        machine.setId(UUID.randomUUID());
        machine.setName("CNC Mill 01");

        MachineHealthAssessment assessment = new MachineHealthAssessment();
        assessment.setId(UUID.randomUUID());
        assessment.setMachine(machine);
        assessment.setHealthScore(92);
        assessment.setHealthStatus(MachineHealthStatus.EXCELLENT);
        assessment.setIsoSeverityZone(IsoSeverityZone.ZONE_A);

        Optional<WorkOrderDto> result = service.evaluateAndCreatePrescriptiveOrder(assessment, null, List.of(), null);

        assertTrue(result.isEmpty());
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void evaluateAndCreatePrescriptiveOrder_DegradedMachineWarning_CreatesHighPriorityOrder() {
        UUID machineId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(machineId);
        machine.setName("Stamping Press 04");
        machine.setSerialNumber("SP-9921");

        MachineHealthAssessment assessment = new MachineHealthAssessment();
        assessment.setId(UUID.randomUUID());
        assessment.setMachine(machine);
        assessment.setHealthScore(52); // < 60%
        assessment.setHealthStatus(MachineHealthStatus.WARNING);
        assessment.setIsoSeverityZone(IsoSeverityZone.ZONE_C);
        assessment.setDominantFaultType(FaultHarmonicType.MISALIGNMENT_2X.name());
        assessment.setRmsVelocityMmS(4.5);
        assessment.setDiagnosisSummary("Shaft misalignment detected");
        assessment.setRecommendedAction("Check laser alignment and coupling spider");

        VibrationBurstSample burst = new VibrationBurstSample();
        burst.setAxis("RADIAL_X");
        burst.setCrestFactor(3.8);
        burst.setKurtosis(4.2);
        burst.setRunningSpeedRpm(3000.0);

        when(workOrderRepository.findActivePrescriptiveOrdersForMachineSince(eq(machineId), any(Instant.class)))
                .thenReturn(List.of());

        User tech = new User();
        tech.setId(UUID.randomUUID());
        tech.setDisplayName("John Technician");
        tech.setActive(true);

        when(userRepository.searchUsers(eq(RoleType.TECHNICIAN), eq(true), isNull(), eq(PageRequest.of(0, 1))))
                .thenReturn(new PageImpl<>(List.of(tech)));

        when(workOrderRepository.save(any(MaintenanceWorkOrder.class))).thenAnswer(i -> {
            MaintenanceWorkOrder order = i.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        Optional<WorkOrderDto> result = service.evaluateAndCreatePrescriptiveOrder(assessment, burst, List.of(), null);

        assertTrue(result.isPresent());
        WorkOrderDto wo = result.get();
        assertTrue(wo.isPrescriptive());
        assertEquals(MaintenancePriority.HIGH, wo.getPriority());
        assertEquals(MaintenanceStatus.ASSIGNED, wo.getStatus());
        assertEquals(tech.getId(), wo.getAssignedToId());
        assertNotNull(wo.getDiagnosticSnapshot());
        assertTrue(wo.getSuspectedSubsystem().contains("Shaft Coupling"));
        assertTrue(wo.getRecommendedParts().contains("Coupling Spider"));

        verify(auditRecordingService).record(isNull(), eq("PRESCRIPTIVE_MAINTENANCE_TRIGGERED"), eq("MaintenanceWorkOrder"), any(), isNull(), anyMap());
    }

    @Test
    void evaluateAndCreatePrescriptiveOrder_CriticalBearingSpall_CreatesCriticalPriorityOrder() {
        UUID machineId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(machineId);
        machine.setName("Lathe CNC 02");
        machine.setSerialNumber("LT-3301");

        MachineHealthAssessment assessment = new MachineHealthAssessment();
        assessment.setId(UUID.randomUUID());
        assessment.setMachine(machine);
        assessment.setHealthScore(28); // Critical
        assessment.setHealthStatus(MachineHealthStatus.CRITICAL);
        assessment.setIsoSeverityZone(IsoSeverityZone.ZONE_D);
        assessment.setDominantFaultType(FaultHarmonicType.BPFO_BEARING_OUTER.name());
        assessment.setRmsVelocityMmS(8.9);

        VibrationBurstSample burst = new VibrationBurstSample();
        burst.setAxis("RADIAL_Y");
        burst.setCrestFactor(5.5);
        burst.setKurtosis(6.8);

        when(workOrderRepository.findActivePrescriptiveOrdersForMachineSince(eq(machineId), any(Instant.class)))
                .thenReturn(List.of());
        when(userRepository.searchUsers(eq(RoleType.TECHNICIAN), eq(true), isNull(), eq(PageRequest.of(0, 1))))
                .thenReturn(new PageImpl<>(List.of()));
        when(workOrderRepository.save(any(MaintenanceWorkOrder.class))).thenAnswer(i -> {
            MaintenanceWorkOrder order = i.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        Optional<WorkOrderDto> result = service.evaluateAndCreatePrescriptiveOrder(assessment, burst, List.of(), null);

        assertTrue(result.isPresent());
        WorkOrderDto wo = result.get();
        assertEquals(MaintenancePriority.CRITICAL, wo.getPriority());
        assertEquals(MaintenanceStatus.OPEN, wo.getStatus());
        assertTrue(wo.getSuspectedSubsystem().contains("Bearing Assembly"));
        assertTrue(wo.getRecommendedParts().contains("SKF 7014"));
    }

    @Test
    void evaluateAndCreatePrescriptiveOrder_DuplicateTriggerWithin24Hours_DeduplicatesExistingOrder() {
        UUID machineId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(machineId);
        machine.setName("CNC Mill 01");

        MachineHealthAssessment assessment = new MachineHealthAssessment();
        assessment.setId(UUID.randomUUID());
        assessment.setMachine(machine);
        assessment.setHealthScore(45);
        assessment.setHealthStatus(MachineHealthStatus.WARNING);
        assessment.setIsoSeverityZone(IsoSeverityZone.ZONE_C);
        assessment.setDominantFaultType(FaultHarmonicType.BPFO_BEARING_OUTER.name());

        MaintenanceWorkOrder existingOrder = new MaintenanceWorkOrder();
        existingOrder.setId(UUID.randomUUID());
        existingOrder.setWorkOrderNumber("WO-PREDICT-12345");
        existingOrder.setMachine(machine);
        existingOrder.setTitle("PRESCRIPTIVE: Bearing Outer Race Spall (BPFO) on CNC Mill 01");
        existingOrder.setDescription("Existing order description");
        existingOrder.setPriority(MaintenancePriority.HIGH);
        existingOrder.setStatus(MaintenanceStatus.IN_PROGRESS);
        existingOrder.setPrescriptive(true);
        existingOrder.setCreatedAt(Instant.now().minus(2, ChronoUnit.HOURS));

        when(workOrderRepository.findActivePrescriptiveOrdersForMachineSince(eq(machineId), any(Instant.class)))
                .thenReturn(List.of(existingOrder));
        when(workOrderRepository.save(any(MaintenanceWorkOrder.class))).thenAnswer(i -> i.getArgument(0));

        Optional<WorkOrderDto> result = service.evaluateAndCreatePrescriptiveOrder(assessment, null, List.of(), null);

        assertTrue(result.isPresent());
        WorkOrderDto wo = result.get();
        assertEquals("WO-PREDICT-12345", wo.getWorkOrderNumber());
        assertEquals(existingOrder.getId(), wo.getId());

        verify(auditRecordingService).record(isNull(), eq("PRESCRIPTIVE_MAINTENANCE_DEDUPLICATED"), eq("MaintenanceWorkOrder"), eq(existingOrder.getId()), isNull(), anyMap());
    }
}
