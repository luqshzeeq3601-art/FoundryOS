package com.factoryos.modules.downtime.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.Role;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.downtime.domain.DowntimeEvent;
import com.factoryos.modules.downtime.domain.DowntimeReasonCode;
import com.factoryos.modules.downtime.domain.DowntimeTriggerSource;
import com.factoryos.modules.downtime.dto.AcknowledgeRootCauseRequest;
import com.factoryos.modules.downtime.dto.AutomatedEvaluationResultDto;
import com.factoryos.modules.downtime.dto.DowntimeEventDto;
import com.factoryos.modules.downtime.dto.MicroStopSummaryDto;
import com.factoryos.modules.downtime.repository.DowntimeEventRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutomatedDowntimeDetectionServiceTest {

    @Mock
    private DowntimeEventRepository downtimeEventRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private AuditRecordingService auditRecordingService;

    @InjectMocks
    private AutomatedDowntimeDetectionService automatedDowntimeService;

    private Machine testMachine;
    private ProductionOrder activeOrder;
    private User testOperator;
    private UUID machineId;

    @BeforeEach
    void setUp() {
        machineId = UUID.randomUUID();
        testMachine = new Machine();
        testMachine.setId(machineId);
        testMachine.setName("CNC Mill Alpha");
        testMachine.setSerialNumber("CNC-001");
        testMachine.setStatus(MachineStatus.RUNNING);

        activeOrder = new ProductionOrder();
        activeOrder.setId(UUID.randomUUID());
        activeOrder.setOrderNumber("PO-2026-001");
        activeOrder.setMachine(testMachine);
        activeOrder.setStatus(ProductionOrderStatus.IN_PROGRESS);

        testOperator = new User();
        testOperator.setId(UUID.randomUUID());
        testOperator.setEmail("operator@factoryos.local");
        testOperator.setDisplayName("Joe Operator");
        testOperator.setRole(new Role(RoleType.OPERATOR));
    }

    @Test
    @DisplayName("Should automatically trigger DOWN transition when speed/cycle stops during active order")
    void testAutomaticDownTransitionWhenSensorPulseStopsDuringActiveOrder() {
        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));
        when(productionOrderRepository.findByMachineIdAndStatus(machineId, ProductionOrderStatus.IN_PROGRESS))
                .thenReturn(Optional.of(activeOrder));
        when(downtimeEventRepository.findByMachineIdAndEndTimeIsNull(machineId)).thenReturn(Optional.empty());

        DowntimeEvent mockSavedEvent = new DowntimeEvent();
        mockSavedEvent.setId(UUID.randomUUID());
        when(downtimeEventRepository.save(any(DowntimeEvent.class))).thenReturn(mockSavedEvent);

        Instant now = Instant.now();
        AutomatedEvaluationResultDto result = automatedDowntimeService.evaluateMachineStream(
                machineId, 0.0, 0.0, now
        );

        assertThat(result.getActionTaken()).isEqualTo("TRIGGERED_DOWN");
        assertThat(testMachine.getStatus()).isEqualTo(MachineStatus.DOWN);

        ArgumentCaptor<DowntimeEvent> eventCaptor = ArgumentCaptor.forClass(DowntimeEvent.class);
        verify(downtimeEventRepository).save(eventCaptor.capture());

        DowntimeEvent captured = eventCaptor.getValue();
        assertThat(captured.getTriggerSource()).isEqualTo(DowntimeTriggerSource.AUTOMATED_SENSOR);
        assertThat(captured.getReasonCode()).isEqualTo(DowntimeReasonCode.MICRO_STOP);
        assertThat(captured.isMicroStop()).isFalse();
        assertThat(captured.getStartTime()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should not transition machine when machine stops without active production order")
    void testNoTransitionWhenMachineStopsWithoutActiveOrder() {
        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));
        when(productionOrderRepository.findByMachineIdAndStatus(machineId, ProductionOrderStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());

        AutomatedEvaluationResultDto result = automatedDowntimeService.evaluateMachineStream(
                machineId, 0.0, 0.0, Instant.now()
        );

        assertThat(result.getActionTaken()).isEqualTo("NO_ACTIVE_ORDER");
        assertThat(testMachine.getStatus()).isEqualTo(MachineStatus.RUNNING);
        verify(downtimeEventRepository, never()).save(any(DowntimeEvent.class));
    }

    @Test
    @DisplayName("Should automatically resolve micro-stop when cycling resumes within 180 seconds")
    void testMicroStopAutoResolvedWhenCyclingResumesUnder180Seconds() {
        testMachine.setStatus(MachineStatus.DOWN);

        Instant startTime = Instant.now().minus(Duration.ofSeconds(45));
        DowntimeEvent openEvent = new DowntimeEvent();
        openEvent.setId(UUID.randomUUID());
        openEvent.setMachine(testMachine);
        openEvent.setStartTime(startTime);
        openEvent.setTriggerSource(DowntimeTriggerSource.AUTOMATED_SENSOR);
        openEvent.setReasonCode(DowntimeReasonCode.MICRO_STOP);

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));
        when(productionOrderRepository.findByMachineIdAndStatus(machineId, ProductionOrderStatus.IN_PROGRESS))
                .thenReturn(Optional.of(activeOrder));
        when(downtimeEventRepository.findByMachineIdAndEndTimeIsNull(machineId)).thenReturn(Optional.of(openEvent));

        Instant now = Instant.now();
        // Spindle speed resumes to 8500 RPM
        AutomatedEvaluationResultDto result = automatedDowntimeService.evaluateMachineStream(
                machineId, 8500.0, 1.0, now
        );

        assertThat(result.getActionTaken()).isEqualTo("RESOLVED_MICRO_STOP");
        assertThat(openEvent.getEndTime()).isEqualTo(now);
        assertThat(openEvent.isMicroStop()).isTrue();
        assertThat(openEvent.getResolutionNote()).contains("Auto-resolved micro-stop");
        assertThat(testMachine.getStatus()).isEqualTo(MachineStatus.RUNNING);

        verify(downtimeEventRepository).save(openEvent);
        verify(machineRepository).save(testMachine);
    }

    @Test
    @DisplayName("Should trigger operator prompt when downtime duration crosses 180 seconds")
    void testOperatorPromptTriggeredWhenStoppageCrosses180Seconds() {
        testMachine.setStatus(MachineStatus.DOWN);

        Instant startTime = Instant.now().minus(Duration.ofSeconds(200));
        DowntimeEvent openEvent = new DowntimeEvent();
        openEvent.setId(UUID.randomUUID());
        openEvent.setMachine(testMachine);
        openEvent.setStartTime(startTime);
        openEvent.setTriggerSource(DowntimeTriggerSource.AUTOMATED_SENSOR);
        openEvent.setReasonCode(DowntimeReasonCode.MICRO_STOP);
        openEvent.setRootCausePromptedAt(null);

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));
        when(productionOrderRepository.findByMachineIdAndStatus(machineId, ProductionOrderStatus.IN_PROGRESS))
                .thenReturn(Optional.of(activeOrder));
        when(downtimeEventRepository.findByMachineIdAndEndTimeIsNull(machineId)).thenReturn(Optional.of(openEvent));

        Instant now = Instant.now();
        // Still stopped (0 RPM)
        AutomatedEvaluationResultDto result = automatedDowntimeService.evaluateMachineStream(
                machineId, 0.0, 0.0, now
        );

        assertThat(result.getActionTaken()).isEqualTo("FLAGGED_OPERATOR_PROMPT");
        assertThat(openEvent.getRootCausePromptedAt()).isEqualTo(now);
        assertThat(testMachine.getStatus()).isEqualTo(MachineStatus.DOWN);

        verify(downtimeEventRepository).save(openEvent);
    }

    @Test
    @DisplayName("Should allow operator to acknowledge root cause and transition machine upon resume")
    void testOperatorAcknowledgesRootCauseAndMachineResumes() {
        UUID eventId = UUID.randomUUID();
        DowntimeEvent openEvent = new DowntimeEvent();
        openEvent.setId(eventId);
        openEvent.setMachine(testMachine);
        openEvent.setStartTime(Instant.now().minus(Duration.ofMinutes(5)));
        openEvent.setReasonCode(DowntimeReasonCode.MICRO_STOP);
        openEvent.setRootCausePromptedAt(Instant.now().minus(Duration.ofMinutes(2)));
        openEvent.setVersion(1L);

        when(downtimeEventRepository.findById(eventId)).thenReturn(Optional.of(openEvent));
        when(downtimeEventRepository.save(any(DowntimeEvent.class))).thenReturn(openEvent);

        AcknowledgeRootCauseRequest req = new AcknowledgeRootCauseRequest(
                DowntimeReasonCode.TOOLING_JAM,
                "Operator cleared jammed feeder rail",
                1L
        );

        DowntimeEventDto ackResult = automatedDowntimeService.acknowledgeRootCause(eventId, req, testOperator);

        assertThat(ackResult.getReasonCode()).isEqualTo(DowntimeReasonCode.TOOLING_JAM);
        assertThat(openEvent.getRootCauseAcknowledgedAt()).isNotNull();
        assertThat(openEvent.getDescription()).isEqualTo("Operator cleared jammed feeder rail");

        // Now simulate machine cycling resuming
        testMachine.setStatus(MachineStatus.DOWN);
        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));
        when(productionOrderRepository.findByMachineIdAndStatus(machineId, ProductionOrderStatus.IN_PROGRESS))
                .thenReturn(Optional.of(activeOrder));
        when(downtimeEventRepository.findByMachineIdAndEndTimeIsNull(machineId)).thenReturn(Optional.of(openEvent));

        Instant resumeTime = Instant.now();
        AutomatedEvaluationResultDto evalResult = automatedDowntimeService.evaluateMachineStream(
                machineId, 7200.0, 1.0, resumeTime
        );

        assertThat(evalResult.getActionTaken()).isEqualTo("RESOLVED_AFTER_ACK");
        assertThat(openEvent.getEndTime()).isEqualTo(resumeTime);
        assertThat(openEvent.isMicroStop()).isFalse();
        assertThat(testMachine.getStatus()).isEqualTo(MachineStatus.RUNNING);
    }

    @Test
    @DisplayName("Should compute micro-stop vs major downtime statistics correctly")
    void testMicroStopSummaryCalculation() {
        Instant from = Instant.now().minus(Duration.ofHours(8));
        Instant to = Instant.now();

        DowntimeEvent micro1 = new DowntimeEvent();
        micro1.setStartTime(from.plus(Duration.ofMinutes(10)));
        micro1.setEndTime(from.plus(Duration.ofMinutes(11))); // 60s
        micro1.setMicroStop(true);

        DowntimeEvent micro2 = new DowntimeEvent();
        micro2.setStartTime(from.plus(Duration.ofMinutes(30)));
        micro2.setEndTime(from.plus(Duration.ofMinutes(32))); // 120s
        micro2.setMicroStop(true);

        DowntimeEvent major1 = new DowntimeEvent();
        major1.setStartTime(from.plus(Duration.ofHours(2)));
        major1.setEndTime(from.plus(Duration.ofHours(2)).plus(Duration.ofMinutes(45))); // 2700s
        major1.setMicroStop(false);

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(testMachine));
        when(downtimeEventRepository.findEventsInInterval(machineId, from, to))
                .thenReturn(List.of(micro1, micro2, major1));

        MicroStopSummaryDto summary = automatedDowntimeService.getMicroStopSummary(machineId, from, to);

        assertThat(summary.getMicroStopCount()).isEqualTo(2);
        assertThat(summary.getTotalMicroStopSeconds()).isEqualTo(180);
        assertThat(summary.getMajorDowntimeCount()).isEqualTo(1);
        assertThat(summary.getTotalMajorDowntimeSeconds()).isEqualTo(2700);
        assertThat(summary.getMicroStopPercentage()).isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.01));
    }
}
