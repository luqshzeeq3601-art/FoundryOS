package com.factoryos.modules.tenant.application;

import com.factoryos.common.dto.PagedResponse;
import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.application.AuthService;
import com.factoryos.modules.auth.domain.Role;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.dto.LoginResponse;
import com.factoryos.modules.machine.application.MachineService;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.dto.CreateMachineRequest;
import com.factoryos.modules.machine.dto.MachineDto;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.application.ProductionOrderService;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.dto.CreateProductionOrderRequest;
import com.factoryos.modules.production.dto.ProductionOrderDto;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import com.factoryos.modules.tenant.context.TenantContext;
import com.factoryos.modules.tenant.context.TenantContextHolder;
import com.factoryos.modules.tenant.domain.Enterprise;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.domain.UserPlantMembership;
import com.factoryos.modules.tenant.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrossTenantSecurityIntegrationTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private ProductionOrderRepository productionOrderRepository;

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

    @Mock
    private UserPlantMembershipRepository membershipRepository;

    @Mock
    private com.factoryos.modules.sop.application.QualityGateService qualityGateService;

    private MachineService machineService;
    private ProductionOrderService productionOrderService;
    private AuthService authService;

    private Enterprise globalEnterprise;
    private Plant austinPlant;
    private Plant berlinPlant;

    private User austinOperator;
    private User berlinOperator;
    private User globalAdmin;

    private Machine austinMachine;
    private Machine berlinMachine;

    @BeforeEach
    void setUp() {
        machineService = new MachineService(
                machineRepository, auditRecordingService, plantRepository,
                areaRepository, lineRepository, workCellRepository
        );

        productionOrderService = new ProductionOrderService(
                productionOrderRepository, machineRepository, auditRecordingService, qualityGateService
        );

        authService = new AuthService(
                null, null, null,
                new com.factoryos.modules.auth.infrastructure.JwtTokenService("development_only_secret_key_must_be_at_least_256_bits_long_for_security_12345", 10),
                membershipRepository, plantRepository
        );

        globalEnterprise = new Enterprise(UUID.randomUUID(), "ENT-GLOBAL", "FactoryOS Global", "HQ");

        austinPlant = new Plant(UUID.fromString("00000000-0000-0000-0000-000000000201"), globalEnterprise, "PLANT-AUSTIN-01", "Austin Gigafactory", "America/Chicago", "Austin TX", "ACTIVE");
        berlinPlant = new Plant(UUID.fromString("00000000-0000-0000-0000-000000000202"), globalEnterprise, "PLANT-BERLIN-02", "Berlin Advanced", "Europe/Berlin", "Berlin DE", "ACTIVE");

        Role operatorRole = new Role();
        operatorRole.setId(UUID.randomUUID());
        operatorRole.setName(RoleType.OPERATOR);

        Role adminRole = new Role();
        adminRole.setId(UUID.randomUUID());
        adminRole.setName(RoleType.ADMIN);

        austinOperator = new User();
        austinOperator.setId(UUID.randomUUID());
        austinOperator.setEmail("operator@austin.factoryos.com");
        austinOperator.setDisplayName("Austin Operator");
        austinOperator.setRole(operatorRole);

        berlinOperator = new User();
        berlinOperator.setId(UUID.randomUUID());
        berlinOperator.setEmail("operator@berlin.factoryos.com");
        berlinOperator.setDisplayName("Berlin Operator");
        berlinOperator.setRole(operatorRole);

        globalAdmin = new User();
        globalAdmin.setId(UUID.randomUUID());
        globalAdmin.setEmail("admin@factoryos.com");
        globalAdmin.setDisplayName("Global Admin");
        globalAdmin.setRole(adminRole);

        austinMachine = new Machine();
        austinMachine.setId(UUID.randomUUID());
        austinMachine.setSerialNumber("CNC-AUSTIN-001");
        austinMachine.setName("Austin 5-Axis Mill");
        austinMachine.setLocation("Bay A-1");
        austinMachine.setPlant(austinPlant);

        berlinMachine = new Machine();
        berlinMachine.setId(UUID.randomUUID());
        berlinMachine.setSerialNumber("CNC-BERLIN-001");
        berlinMachine.setName("Berlin Stamping Cell");
        berlinMachine.setLocation("Hall 4");
        berlinMachine.setPlant(berlinPlant);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldAllowAustinOperatorToViewAustinMachine() {
        // Set context to Austin
        TenantContextHolder.setContext(new TenantContext(
                globalEnterprise.getId(), austinPlant.getId(), austinPlant.getCode(),
                austinPlant.getName(), "OPERATOR", false, List.of(austinPlant.getId())
        ));

        when(machineRepository.findByIdAndIsDeletedFalse(austinMachine.getId())).thenReturn(Optional.of(austinMachine));

        MachineDto dto = machineService.getMachineById(austinMachine.getId());
        assertNotNull(dto);
        assertEquals("CNC-AUSTIN-001", dto.getSerialNumber());
        assertEquals("PLANT-AUSTIN-01", dto.getPlantCode());
    }

    @Test
    void shouldBlockAustinOperatorFromAccessingBerlinMachine() {
        // Set context to Austin
        TenantContextHolder.setContext(new TenantContext(
                globalEnterprise.getId(), austinPlant.getId(), austinPlant.getCode(),
                austinPlant.getName(), "OPERATOR", false, List.of(austinPlant.getId())
        ));

        when(machineRepository.findByIdAndIsDeletedFalse(berlinMachine.getId())).thenReturn(Optional.of(berlinMachine));

        // Austin operator querying Berlin machine must be rejected with Forbidden!
        AppException exception = assertThrows(AppException.class, () ->
                machineService.getMachineById(berlinMachine.getId())
        );

        assertTrue(exception.getMessage().contains("Cross-tenant access violation"));
    }

    @Test
    void shouldFilterMachineListByTenantPlantId() {
        // Set context to Berlin
        TenantContextHolder.setContext(new TenantContext(
                globalEnterprise.getId(), berlinPlant.getId(), berlinPlant.getCode(),
                berlinPlant.getName(), "OPERATOR", false, List.of(berlinPlant.getId())
        ));

        Pageable pageable = PageRequest.of(0, 20);
        when(machineRepository.searchMachinesWithPlant(eq(berlinPlant.getId()), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(berlinMachine)));

        PagedResponse<MachineDto> response = machineService.getMachines(null, null, pageable);

        assertEquals(1, response.getItems().size());
        assertEquals("CNC-BERLIN-001", response.getItems().get(0).getSerialNumber());
        verify(machineRepository).searchMachinesWithPlant(eq(berlinPlant.getId()), isNull(), isNull(), eq(pageable));
    }

    @Test
    void shouldBlockAustinOperatorFromCreatingOrderOnBerlinMachine() {
        // Set context to Austin
        TenantContextHolder.setContext(new TenantContext(
                globalEnterprise.getId(), austinPlant.getId(), austinPlant.getCode(),
                austinPlant.getName(), "OPERATOR", false, List.of(austinPlant.getId())
        ));

        when(productionOrderRepository.existsByOrderNumberAndIsDeletedFalse("ORD-CROSS-001")).thenReturn(false);
        when(machineRepository.findByIdAndIsDeletedFalse(berlinMachine.getId())).thenReturn(Optional.of(berlinMachine));

        CreateProductionOrderRequest req = new CreateProductionOrderRequest();
        req.setOrderNumber("ORD-CROSS-001");
        req.setMachineId(berlinMachine.getId());
        req.setProductCode("BRACKET-01");
        req.setPlannedQuantity(100);

        AppException ex = assertThrows(AppException.class, () ->
                productionOrderService.createProductionOrder(req, austinOperator)
        );

        assertTrue(ex.getMessage().contains("Cross-tenant violation"));
    }

    @Test
    void shouldAllowGlobalAdminToAccessAnyPlantMachine() {
        // Global admin context (no plant scoping restriction)
        TenantContextHolder.setContext(new TenantContext(
                globalEnterprise.getId(), austinPlant.getId(), austinPlant.getCode(),
                austinPlant.getName(), "ADMIN", true, List.of(austinPlant.getId(), berlinPlant.getId())
        ));

        when(machineRepository.findByIdAndIsDeletedFalse(berlinMachine.getId())).thenReturn(Optional.of(berlinMachine));

        // Global admin can view Berlin machine even if current plant header is Austin
        MachineDto dto = machineService.getMachineById(berlinMachine.getId());
        assertNotNull(dto);
        assertEquals("CNC-BERLIN-001", dto.getSerialNumber());
    }

    @Test
    void shouldRejectUnauthorizedPlantSwitching() {
        UserPlantMembership austinMem = new UserPlantMembership(UUID.randomUUID(), austinOperator, austinPlant, austinOperator.getRole(), true);
        when(membershipRepository.findByUserId(austinOperator.getId())).thenReturn(List.of(austinMem));
        when(plantRepository.findByIdAndIsDeletedFalse(berlinPlant.getId())).thenReturn(Optional.of(berlinPlant));

        // Austin operator attempts to switch to Berlin
        AppException ex = assertThrows(AppException.class, () ->
                authService.switchPlant(austinOperator, berlinPlant.getId())
        );

        assertTrue(ex.getMessage().contains("Cross-tenant access violation"));
    }

    @Test
    void shouldAllowAuthorizedPlantSwitchingForDualMembershipUser() {
        UserPlantMembership austinMem = new UserPlantMembership(UUID.randomUUID(), austinOperator, austinPlant, austinOperator.getRole(), true);
        UserPlantMembership berlinMem = new UserPlantMembership(UUID.randomUUID(), austinOperator, berlinPlant, austinOperator.getRole(), false);

        when(membershipRepository.findByUserId(austinOperator.getId())).thenReturn(List.of(austinMem, berlinMem));
        when(plantRepository.findByIdAndIsDeletedFalse(berlinPlant.getId())).thenReturn(Optional.of(berlinPlant));

        LoginResponse response = authService.switchPlant(austinOperator, berlinPlant.getId());

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertEquals(berlinPlant.getId(), response.getUser().getActivePlantId());
        assertEquals("PLANT-BERLIN-02", response.getUser().getActivePlantCode());
    }
}
