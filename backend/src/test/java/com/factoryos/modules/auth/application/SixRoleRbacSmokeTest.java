package com.factoryos.modules.auth.application;

import com.factoryos.common.dto.PagedResponse;
import com.factoryos.modules.audit.application.AuditController;
import com.factoryos.modules.audit.application.AuditQueryService;
import com.factoryos.modules.audit.dto.AuditDto;
import com.factoryos.modules.auth.api.UserController;
import com.factoryos.modules.auth.dto.UserDto;
import com.factoryos.modules.auth.infrastructure.JwtAuthenticationFilter;
import com.factoryos.modules.auth.infrastructure.JwtTokenService;
import com.factoryos.modules.auth.infrastructure.RateLimitingFilter;
import com.factoryos.modules.auth.infrastructure.SecurityConfig;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.downtime.application.DowntimeController;
import com.factoryos.modules.downtime.application.DowntimeService;
import com.factoryos.modules.machine.application.MachineController;
import com.factoryos.modules.machine.application.MachineService;
import com.factoryos.modules.machine.dto.MachineDto;
import com.factoryos.modules.maintenance.application.MaintenanceController;
import com.factoryos.modules.maintenance.application.MaintenanceService;
import com.factoryos.modules.maintenance.dto.WorkOrderDto;
import com.factoryos.modules.operations.application.OperationsController;
import com.factoryos.modules.operations.application.OperationsCoordinator;
import com.factoryos.modules.production.application.ProductionOrderController;
import com.factoryos.modules.production.application.ProductionOrderService;
import com.factoryos.modules.production.dto.ProductionOrderDto;
import com.factoryos.modules.reporting.application.DashboardController;
import com.factoryos.modules.reporting.application.DashboardReportingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        UserController.class,
        AuditController.class,
        MachineController.class,
        ProductionOrderController.class,
        MaintenanceController.class,
        DowntimeController.class,
        OperationsController.class,
        DashboardController.class
})
@Import(SecurityConfig.class)
class SixRoleRbacSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuditQueryService auditQueryService;

    @MockBean
    private MachineService machineService;

    @MockBean
    private ProductionOrderService productionOrderService;

    @MockBean
    private MaintenanceService maintenanceService;

    @MockBean
    private DowntimeService downtimeService;

    @MockBean
    private OperationsCoordinator operationsCoordinator;

    @MockBean
    private DashboardReportingService dashboardReportingService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private com.factoryos.modules.tenant.context.TenantContextFilter tenantContextFilter;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private UserRepository userRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUpFilters() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.ServletRequest req = invocation.getArgument(0);
            jakarta.servlet.ServletResponse res = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.ServletRequest req = invocation.getArgument(0);
            jakarta.servlet.ServletResponse res = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(rateLimitingFilter).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.ServletRequest req = invocation.getArgument(0);
            jakarta.servlet.ServletResponse res = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(tenantContextFilter).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    // -------------------------------------------------------------
    // 1. ADMIN ROLE TESTS
    // -------------------------------------------------------------
    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_CanAccessAllEndpoints() throws Exception {
        when(userService.getUsers(any(), any(), any(), any()))
                .thenReturn(new PagedResponse<UserDto>(Collections.emptyList(), 0, 20, 0L, 0));
        when(machineService.getMachines(any(), any(), any()))
                .thenReturn(new PagedResponse<MachineDto>(Collections.emptyList(), 0, 20, 0L, 0));
        when(auditQueryService.getAuditEvents(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PagedResponse<AuditDto>(Collections.emptyList(), 0, 30, 0L, 0));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/audit-events"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/machines"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/machines/" + UUID.randomUUID())
                        .param("expectedVersion", "0"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------
    // 2. PRODUCTION MANAGER ROLE TESTS
    // -------------------------------------------------------------
    @Test
    @WithMockUser(roles = "PRODUCTION_MANAGER")
    void productionManager_CanManageOrdersAndAudit_ForbiddenOnUserAdmin() throws Exception {
        when(productionOrderService.getProductionOrders(any(), any(), any(), any()))
                .thenReturn(new PagedResponse<ProductionOrderDto>(Collections.emptyList(), 0, 20, 0L, 0));
        when(auditQueryService.getAuditEvents(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PagedResponse<AuditDto>(Collections.emptyList(), 0, 30, 0L, 0));

        // Can access production and audit
        mockMvc.perform(get("/api/v1/production-orders"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/audit-events"))
                .andExpect(status().isOk());

        // Cannot manage users or delete machines
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/machines/" + UUID.randomUUID())
                        .param("expectedVersion", "0"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------
    // 3. ENGINEER ROLE TESTS
    // -------------------------------------------------------------
    @Test
    @WithMockUser(roles = "ENGINEER")
    void engineer_CanManageMachinesAndMaintenance_ForbiddenOnUserAdmin() throws Exception {
        when(machineService.getMachines(any(), any(), any()))
                .thenReturn(new PagedResponse<MachineDto>(Collections.emptyList(), 0, 20, 0L, 0));
        when(auditQueryService.getAuditEvents(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PagedResponse<AuditDto>(Collections.emptyList(), 0, 30, 0L, 0));

        mockMvc.perform(get("/api/v1/machines"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/audit-events"))
                .andExpect(status().isOk());

        // Cannot create production orders
        mockMvc.perform(post("/api/v1/production-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"machineId\":\"" + UUID.randomUUID() + "\",\"productCode\":\"P1\",\"plannedQuantity\":100}"))
                .andExpect(status().isForbidden());

        // Cannot manage users
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------
    // 4. TECHNICIAN ROLE TESTS
    // -------------------------------------------------------------
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void technician_CanManageMaintenanceAndDowntime_ForbiddenOnAuditAndUserAdmin() throws Exception {
        when(maintenanceService.getWorkOrders(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PagedResponse<WorkOrderDto>(Collections.emptyList(), 0, 20, 0L, 0));

        mockMvc.perform(get("/api/v1/maintenance-work-orders"))
                .andExpect(status().isOk());

        // Forbidden on user admin
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());

        // Forbidden on audit query
        mockMvc.perform(get("/api/v1/audit-events"))
                .andExpect(status().isForbidden());

        // Forbidden on production order creation
        mockMvc.perform(post("/api/v1/production-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"machineId\":\"" + UUID.randomUUID() + "\",\"productCode\":\"P1\",\"plannedQuantity\":100}"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------
    // 5. OPERATOR ROLE TESTS
    // -------------------------------------------------------------
    @Test
    @WithMockUser(roles = "OPERATOR")
    void operator_CanViewDashboardAndPerformOperations_ForbiddenOnAdminAndAudit() throws Exception {
        when(machineService.getMachines(any(), any(), any()))
                .thenReturn(new PagedResponse<MachineDto>(Collections.emptyList(), 0, 20, 0L, 0));

        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/machines"))
                .andExpect(status().isOk());

        // Forbidden on user admin
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());

        // Forbidden on audit queries
        mockMvc.perform(get("/api/v1/audit-events"))
                .andExpect(status().isForbidden());

        // Forbidden on assigning maintenance
        mockMvc.perform(post("/api/v1/maintenance-work-orders/" + UUID.randomUUID() + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedTo\":\"" + UUID.randomUUID() + "\",\"expectedVersion\":0}"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------
    // 6. VIEWER ROLE TESTS (READ-ONLY)
    // -------------------------------------------------------------
    @Test
    @WithMockUser(roles = "VIEWER")
    void viewer_CanOnlyReadOperationalData_ForbiddenOnAllMutations() throws Exception {
        when(machineService.getMachines(any(), any(), any()))
                .thenReturn(new PagedResponse<MachineDto>(Collections.emptyList(), 0, 20, 0L, 0));
        when(productionOrderService.getProductionOrders(any(), any(), any(), any()))
                .thenReturn(new PagedResponse<ProductionOrderDto>(Collections.emptyList(), 0, 20, 0L, 0));

        // Read endpoints succeed
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/machines"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/production-orders"))
                .andExpect(status().isOk());

        // Mutation endpoints are forbidden
        mockMvc.perform(post("/api/v1/machines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serialNumber\":\"M-99\",\"name\":\"Test\",\"location\":\"Bay 1\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/production-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"machineId\":\"" + UUID.randomUUID() + "\",\"productCode\":\"P1\",\"plannedQuantity\":100}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/downtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"machineId\":\"" + UUID.randomUUID() + "\",\"reasonCode\":\"BREAKDOWN\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/audit-events"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }
}
