package com.factoryos.modules.maintenance.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.Role;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.downtime.repository.DowntimeEventRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.maintenance.domain.MaintenancePriority;
import com.factoryos.modules.maintenance.domain.MaintenanceStatus;
import com.factoryos.modules.maintenance.domain.MaintenanceWorkOrder;
import com.factoryos.modules.maintenance.dto.AssignWorkOrderRequest;
import com.factoryos.modules.maintenance.dto.CancelWorkOrderRequest;
import com.factoryos.modules.maintenance.dto.CompleteWorkOrderRequest;
import com.factoryos.modules.maintenance.dto.CreateWorkOrderRequest;
import com.factoryos.modules.maintenance.dto.WorkOrderDto;
import com.factoryos.modules.maintenance.repository.MaintenanceWorkOrderRepository;
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
class MaintenanceServiceTest {

    @Mock
    private MaintenanceWorkOrderRepository maintenanceWorkOrderRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private DowntimeEventRepository downtimeEventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditRecordingService auditRecordingService;

    private MaintenanceService maintenanceService;

    @BeforeEach
    void setUp() {
        maintenanceService = new MaintenanceService(
                maintenanceWorkOrderRepository,
                machineRepository,
                downtimeEventRepository,
                userRepository,
                auditRecordingService
        );
    }

    @Test
    void createWorkOrder_Success_InitializesOpen() {
        UUID machineId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(machineId);
        machine.setName("Stamping Press 2");

        when(maintenanceWorkOrderRepository.existsByWorkOrderNumberAndIsDeletedFalse("WO-001")).thenReturn(false);
        when(machineRepository.findByIdAndIsDeletedFalse(machineId)).thenReturn(Optional.of(machine));

        MaintenanceWorkOrder saved = new MaintenanceWorkOrder();
        saved.setId(UUID.randomUUID());
        saved.setWorkOrderNumber("WO-001");
        saved.setMachine(machine);
        saved.setTitle("Replace Drive Belt");
        saved.setDescription("Check tension and alignment");
        saved.setPriority(MaintenancePriority.HIGH);
        saved.setStatus(MaintenanceStatus.OPEN);
        when(maintenanceWorkOrderRepository.save(any(MaintenanceWorkOrder.class))).thenReturn(saved);

        CreateWorkOrderRequest request = new CreateWorkOrderRequest();
        request.setWorkOrderNumber("WO-001");
        request.setMachineId(machineId);
        request.setTitle("Replace Drive Belt");
        request.setDescription("Check tension and alignment");
        request.setPriority(MaintenancePriority.HIGH);

        WorkOrderDto result = maintenanceService.createWorkOrder(request, null);

        assertNotNull(result);
        assertEquals(MaintenanceStatus.OPEN, result.getStatus());
        assertEquals(MaintenancePriority.HIGH, result.getPriority());
        verify(auditRecordingService).record(isNull(), eq("WORK_ORDER_CREATED"), eq("MaintenanceWorkOrder"), eq(saved.getId()), isNull(), anyMap());
    }

    @Test
    void assignWorkOrder_Success_TransitionsToAssigned() {
        UUID woId = UUID.randomUUID();
        UUID techId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(UUID.randomUUID());
        machine.setName("Stamping Press 2");

        MaintenanceWorkOrder order = new MaintenanceWorkOrder();
        order.setId(woId);
        order.setMachine(machine);
        order.setStatus(MaintenanceStatus.OPEN);
        order.setVersion(0L);

        User tech = new User();
        tech.setId(techId);
        tech.setDisplayName("Alex Technician");
        tech.setActive(true);
        tech.setRole(new Role(RoleType.TECHNICIAN));

        when(maintenanceWorkOrderRepository.findByIdAndIsDeletedFalse(woId)).thenReturn(Optional.of(order));
        when(userRepository.findByIdAndIsDeletedFalse(techId)).thenReturn(Optional.of(tech));
        when(maintenanceWorkOrderRepository.save(any(MaintenanceWorkOrder.class))).thenReturn(order);

        AssignWorkOrderRequest request = new AssignWorkOrderRequest();
        request.setAssignedTo(techId);
        request.setExpectedVersion(0L);

        WorkOrderDto result = maintenanceService.assignWorkOrder(woId, request, null);

        assertNotNull(result);
        assertEquals(MaintenanceStatus.ASSIGNED, order.getStatus());
        assertEquals(tech, order.getAssignedTo());
        verify(auditRecordingService).record(isNull(), eq("WORK_ORDER_ASSIGNED"), eq("MaintenanceWorkOrder"), eq(woId), isNull(), anyMap());
    }

    @Test
    void completeWorkOrder_Success_SetsCompletionNoteAndTimestamps() {
        UUID woId = UUID.randomUUID();
        Machine machine = new Machine();
        machine.setId(UUID.randomUUID());
        machine.setName("Stamping Press 2");

        MaintenanceWorkOrder order = new MaintenanceWorkOrder();
        order.setId(woId);
        order.setMachine(machine);
        order.setStatus(MaintenanceStatus.IN_PROGRESS);
        order.setVersion(1L);

        when(maintenanceWorkOrderRepository.findByIdAndIsDeletedFalse(woId)).thenReturn(Optional.of(order));
        when(maintenanceWorkOrderRepository.save(any(MaintenanceWorkOrder.class))).thenReturn(order);

        CompleteWorkOrderRequest request = new CompleteWorkOrderRequest();
        request.setCompletionNote("Replaced belt and calibrated torque to 45Nm.");
        request.setExpectedVersion(1L);

        WorkOrderDto result = maintenanceService.completeWorkOrder(woId, request, null);

        assertNotNull(result);
        assertEquals(MaintenanceStatus.COMPLETED, order.getStatus());
        assertEquals("Replaced belt and calibrated torque to 45Nm.", order.getCompletionNote());
        assertNotNull(order.getCompletedAt());
        assertNotNull(order.getClosedAt());
        verify(auditRecordingService).record(isNull(), eq("WORK_ORDER_COMPLETED"), eq("MaintenanceWorkOrder"), eq(woId), anyMap(), anyMap());
    }
}
