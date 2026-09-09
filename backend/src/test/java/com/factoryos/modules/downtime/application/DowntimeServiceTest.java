package com.factoryos.modules.downtime.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.downtime.domain.DowntimeEvent;
import com.factoryos.modules.downtime.domain.DowntimeReasonCode;
import com.factoryos.modules.downtime.dto.CreateDowntimeRequest;
import com.factoryos.modules.downtime.dto.DowntimeEventDto;
import com.factoryos.modules.downtime.dto.ResolveDowntimeRequest;
import com.factoryos.modules.downtime.repository.DowntimeEventRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DowntimeServiceTest {

    @Mock
    private DowntimeEventRepository downtimeEventRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private AuditRecordingService auditRecordingService;

    private DowntimeService downtimeService;

    @BeforeEach
    void setUp() {
        downtimeService = new DowntimeService(
                downtimeEventRepository,
                machineRepository,
                auditRecordingService
        );
    }

    @Test
    void createDowntimeEvent_Success_TransitionsMachineToDown() {
        UUID machineId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(machineId);
        machine.setName("Press Line 1");
        machine.setStatus(MachineStatus.RUNNING);

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(machine));
        when(downtimeEventRepository.findByMachineAndEndTimeIsNull(machine)).thenReturn(Optional.empty());

        DowntimeEvent savedEvent = new DowntimeEvent();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setMachine(machine);
        savedEvent.setReasonCode(DowntimeReasonCode.BREAKDOWN);
        savedEvent.setStartTime(Instant.now());
        when(downtimeEventRepository.save(any(DowntimeEvent.class))).thenReturn(savedEvent);

        CreateDowntimeRequest request = new CreateDowntimeRequest();
        request.setMachineId(machineId);
        request.setReasonCode(DowntimeReasonCode.BREAKDOWN);
        request.setDescription("Hydraulic hose burst");

        DowntimeEventDto result = downtimeService.createDowntimeEvent(request, null);

        assertNotNull(result);
        assertEquals(MachineStatus.DOWN, machine.getStatus());
        verify(machineRepository).save(machine);
        verify(auditRecordingService).record(isNull(), eq("DOWNTIME_STARTED"), eq("DowntimeEvent"), eq(savedEvent.getId()), isNull(), anyMap());
    }

    @Test
    void createDowntimeEvent_OpenDowntimeAlreadyExists_ThrowsConflict() {
        UUID machineId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(machineId);
        machine.setName("Press Line 1");

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(machine));
        when(downtimeEventRepository.findByMachineAndEndTimeIsNull(machine)).thenReturn(Optional.of(new DowntimeEvent()));

        CreateDowntimeRequest request = new CreateDowntimeRequest();
        request.setMachineId(machineId);
        request.setReasonCode(DowntimeReasonCode.BREAKDOWN);

        AppException ex = assertThrows(AppException.class, () -> downtimeService.createDowntimeEvent(request, null));
        assertEquals("OPEN_DOWNTIME_EXISTS", ex.getErrorCode());
    }

    @Test
    void resolveDowntimeEvent_Success_TransitionsMachineToIdle() {
        UUID eventId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(UUID.randomUUID());
        machine.setName("Press Line 1");
        machine.setStatus(MachineStatus.DOWN);

        DowntimeEvent event = new DowntimeEvent();
        event.setId(eventId);
        event.setMachine(machine);
        event.setReasonCode(DowntimeReasonCode.BREAKDOWN);
        event.setStartTime(Instant.now().minusSeconds(1800));
        event.setVersion(0L);

        when(downtimeEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(downtimeEventRepository.save(any(DowntimeEvent.class))).thenReturn(event);

        ResolveDowntimeRequest request = new ResolveDowntimeRequest();
        request.setResolutionNote("Replaced hydraulic pressure line and tested seals");
        request.setExpectedVersion(0L);

        DowntimeEventDto result = downtimeService.resolveDowntimeEvent(eventId, request, null);

        assertNotNull(result);
        assertNotNull(event.getEndTime());
        assertEquals("Replaced hydraulic pressure line and tested seals", event.getResolutionNote());
        assertEquals(MachineStatus.IDLE, machine.getStatus());
        verify(machineRepository).save(machine);
        verify(auditRecordingService).record(isNull(), eq("DOWNTIME_RESOLVED"), eq("DowntimeEvent"), eq(eventId), anyMap(), anyMap());
    }
}
