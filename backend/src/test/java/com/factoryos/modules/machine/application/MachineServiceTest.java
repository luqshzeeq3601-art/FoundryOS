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
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.repository.PlantRepository;
import com.factoryos.modules.tenant.repository.ProductionAreaRepository;
import com.factoryos.modules.tenant.repository.ProductionLineRepository;
import com.factoryos.modules.tenant.repository.WorkCellRepository;
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

    @Mock
    private PlantRepository plantRepository;

    @Mock
    private ProductionAreaRepository areaRepository;

    @Mock
    private ProductionLineRepository lineRepository;

    @Mock
    private WorkCellRepository workCellRepository;

    private MachineService machineService;
    private Plant defaultPlant;

    @BeforeEach
    void setUp() {
        machineService = new MachineService(
                machineRepository, auditRecordingService, plantRepository,
                areaRepository, lineRepository, workCellRepository
        );

        defaultPlant = new Plant();
        defaultPlant.setId(UUID.fromString("00000000-0000-0000-0000-000000000201"));
        defaultPlant.setCode("PLANT-AUSTIN-01");
        defaultPlant.setName("Austin Gigafactory");
    }

    @Test
    void createMachine_Success_SavesAndAudits() {
        CreateMachineRequest request = new CreateMachineRequest();
        request.setSerialNumber("cnc-01");
        request.setName("CNC Mill 1");
        request.setLocation("Bay A");

        when(machineRepository.existsBySerialNumberAndIsDeletedFalse("CNC-01")).thenReturn(false);
        when(plantRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(defaultPlant));

        Machine savedMachine = new Machine();
        savedMachine.setId(UUID.randomUUID());
        savedMachine.setSerialNumber("CNC-01");
        savedMachine.setName("CNC Mill 1");
        savedMachine.setLocation("Bay A");
        savedMachine.setStatus(MachineStatus.IDLE);
        savedMachine.setPlant(defaultPlant);
        when(machineRepository.save(any(Machine.class))).thenReturn(savedMachine);

        MachineDto dto = machineService.createMachine(request, null);

        assertNotNull(dto);
        assertEquals("CNC-01", dto.getSerialNumber());
        verify(auditRecordingService).record(isNull(), eq("MACHINE_CREATED"), eq("Machine"), eq(savedMachine.getId()), isNull(), anyMap());
    }

    @Test
    void createMachine_DuplicateSerial_ThrowsConflict() {
        CreateMachineRequest request = new CreateMachineRequest();
        request.setSerialNumber("cnc-01");
        request.setName("CNC Mill 1");
        request.setLocation("Bay A");

        when(machineRepository.existsBySerialNumberAndIsDeletedFalse("CNC-01")).thenReturn(true);

        assertThrows(AppException.class, () -> machineService.createMachine(request, null));
        verify(machineRepository, never()).save(any());
    }

    @Test
    void updateMachineStatus_Success_TransitionsAndAudits() {
        UUID machineId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(machineId);
        machine.setStatus(MachineStatus.IDLE);
        machine.setVersion(1L);

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(machine));
        when(machineRepository.save(any(Machine.class))).thenReturn(machine);

        UpdateMachineStatusRequest request = new UpdateMachineStatusRequest();
        request.setStatus(MachineStatus.RUNNING);
        request.setExpectedVersion(1L);

        MachineDto dto = machineService.updateMachineStatus(machineId, request, null);

        assertNotNull(dto);
        assertEquals(MachineStatus.RUNNING, machine.getStatus());
        verify(auditRecordingService).record(isNull(), eq("MACHINE_STATUS_UPDATED"), eq("Machine"), eq(machineId), anyMap(), anyMap());
    }

    @Test
    void updateMachine_VersionConflict_ThrowsOptimisticLockException() {
        UUID machineId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(machineId);
        machine.setVersion(2L);

        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(machine));

        UpdateMachineRequest request = new UpdateMachineRequest();
        request.setName("New Name");
        request.setExpectedVersion(1L); // stale version!

        assertThrows(AppException.class, () -> machineService.updateMachine(machineId, request, null));
        verify(machineRepository, never()).save(any());
    }
}
