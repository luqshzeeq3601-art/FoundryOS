package com.factoryos.modules.machine.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.dto.CreateMachineRequest;
import com.factoryos.modules.machine.dto.MachineDto;
import com.factoryos.modules.machine.dto.UpdateMachineRequest;
import com.factoryos.modules.machine.dto.UpdateMachineStatusRequest;
import com.factoryos.modules.machine.repository.MachineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MachineServiceTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private AuditRecordingService auditRecordingService;

    private MachineService machineService;

    @BeforeEach
    void setUp() {
        machineService = new MachineService(machineRepository, auditRecordingService);
    }

    @Test
    void createMachine_Success_SavesAndAudits() {
        CreateMachineRequest request = new CreateMachineRequest();
        request.setSerialNumber("cnc-01");
        request.setName("CNC Mill 1");
        request.setLocation("Bay A");

        when(machineRepository.existsBySerialNumberAndIsDeletedFalse("CNC-01")).thenReturn(false);

        Machine savedMachine = new Machine();
        savedMachine.setId(UUID.randomUUID());
        savedMachine.setSerialNumber("CNC-01");
        savedMachine.setName("CNC Mill 1");
        savedMachine.setLocation("Bay A");
        savedMachine.setStatus(MachineStatus.IDLE);
        when(machineRepository.save(any(Machine.class))).thenReturn(savedMachine);

        MachineDto dto = machineService.createMachine(request, null);

        assertNotNull(dto);
        assertEquals("CNC-01", dto.getSerialNumber());
        assertEquals(MachineStatus.IDLE, dto.getStatus());
        verify(auditRecordingService).record(isNull(), eq("MACHINE_CREATED"), eq("Machine"), eq(savedMachine.getId()), isNull(), anyMap());
    }

    @Test
    void createMachine_DuplicateSerial_ThrowsConflict() {
        CreateMachineRequest request = new CreateMachineRequest();
        request.setSerialNumber("CNC-01");
        request.setName("CNC Mill 1");
        request.setLocation("Bay A");

        when(machineRepository.existsBySerialNumberAndIsDeletedFalse("CNC-01")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> machineService.createMachine(request, null));
        assertEquals("DUPLICATE_SERIAL", ex.getErrorCode());
    }

    @Test
    void updateMachine_VersionConflict_ThrowsException() {
        UUID id = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(id);
        machine.setVersion(3L);

        when(machineRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.of(machine));

        UpdateMachineRequest request = new UpdateMachineRequest();
        request.setName("Updated Name");
        request.setExpectedVersion(2L); // Stale version

        AppException ex = assertThrows(AppException.class, () -> machineService.updateMachine(id, request, null));
        assertEquals("VERSION_CONFLICT", ex.getErrorCode());
    }

    @Test
    void updateMachineStatus_Success_TransitionsState() {
        UUID id = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(id);
        machine.setStatus(MachineStatus.IDLE);
        machine.setVersion(0L);

        when(machineRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.of(machine));
        when(machineRepository.save(any(Machine.class))).thenReturn(machine);

        UpdateMachineStatusRequest request = new UpdateMachineStatusRequest();
        request.setStatus(MachineStatus.RUNNING);
        request.setExpectedVersion(0L);

        MachineDto result = machineService.updateMachineStatus(id, request, null);

        assertEquals(MachineStatus.RUNNING, result.getStatus());
        verify(auditRecordingService).record(isNull(), eq("MACHINE_STATUS_CHANGED"), eq("Machine"), eq(id), anyMap(), anyMap());
    }
}
