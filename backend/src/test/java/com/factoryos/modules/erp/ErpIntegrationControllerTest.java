package com.factoryos.modules.erp;

import com.factoryos.modules.auth.infrastructure.JwtAuthenticationFilter;
import com.factoryos.modules.auth.infrastructure.JwtTokenService;
import com.factoryos.modules.auth.infrastructure.RateLimitingFilter;
import com.factoryos.modules.auth.infrastructure.SecurityConfig;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.erp.application.ErpIntegrationController;
import com.factoryos.modules.erp.application.ErpSyncService;
import com.factoryos.modules.erp.domain.ErpType;
import com.factoryos.modules.erp.dto.*;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.tenant.context.TenantContextFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ErpIntegrationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class ErpIntegrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ErpSyncService erpSyncService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private TenantContextFilter tenantContextFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(tenantContextFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v2/erp/connectors should return list of connectors")
    void testGetConnectors() throws Exception {
        ErpConnectorDto dto = new ErpConnectorDto();
        dto.setId(UUID.randomUUID());
        dto.setName("Austin SAP Hub");
        dto.setErpType(ErpType.SAP_S4HANA);
        dto.setHealthStatus("HEALTHY");

        when(erpSyncService.getAllConnectors()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v2/erp/connectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Austin SAP Hub"))
                .andExpect(jsonPath("$[0].healthStatus").value("HEALTHY"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v2/erp/connectors/{id}/test should return connection health")
    void testTestConnector() throws Exception {
        UUID id = UUID.randomUUID();
        when(erpSyncService.testConnector(id)).thenReturn(true);

        mockMvc.perform(post("/api/v2/erp/connectors/" + id + "/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("HEALTHY"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v2/erp/connectors/{id}/sync-inbound should return synced order count")
    void testSyncInbound() throws Exception {
        UUID id = UUID.randomUUID();
        ProductionOrder po = new ProductionOrder();
        po.setId(UUID.randomUUID());
        po.setOrderNumber("PO-SAP-1008492");
        po.setStatus(ProductionOrderStatus.DRAFT);

        when(erpSyncService.pollReleasedOrders(id)).thenReturn(List.of(po));

        mockMvc.perform(post("/api/v2/erp/connectors/" + id + "/sync-inbound"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount").value(1))
                .andExpect(jsonPath("$.orders[0]").value("PO-SAP-1008492"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v2/erp/orders/{id}/confirm should post outbound confirmation")
    void testConfirmOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        ErpOrderConfirmationDto dto = new ErpOrderConfirmationDto();
        dto.setId(UUID.randomUUID());
        dto.setProductionOrderId(orderId);
        dto.setConfirmationNumber("CONF-889911");
        dto.setErpPostingStatus("POSTED");
        dto.setErpDocumentNumber("SAP-CONF-12345");

        when(erpSyncService.submitOrderConfirmation(eq(orderId), eq(100), eq(2), eq("SCRAP_CRACK"), eq(4.0), eq(3.5)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v2/erp/orders/" + orderId + "/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmedGoodQty\":100,\"confirmedScrapQty\":2,\"scrapReason\":\"SCRAP_CRACK\",\"laborHours\":4.0,\"machineHours\":3.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.erpPostingStatus").value("POSTED"))
                .andExpect(jsonPath("$.erpDocumentNumber").value("SAP-CONF-12345"));
    }
}
