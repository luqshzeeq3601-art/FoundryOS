package com.factoryos.modules.edge.application;

import com.factoryos.modules.auth.infrastructure.JwtAuthenticationFilter;
import com.factoryos.modules.auth.infrastructure.JwtTokenService;
import com.factoryos.modules.auth.infrastructure.RateLimitingFilter;
import com.factoryos.modules.auth.infrastructure.SecurityConfig;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.edge.domain.SyncStatus;
import com.factoryos.modules.edge.dto.EdgeGatewayDto;
import com.factoryos.modules.edge.dto.EdgeSyncBatchResultDto;
import com.factoryos.modules.edge.service.EdgeGatewayService;
import com.factoryos.modules.edge.service.EdgeStoreAndForwardSyncService;
import com.factoryos.modules.tenant.context.TenantContextFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EdgeController.class)
@Import(SecurityConfig.class)
class EdgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EdgeGatewayService gatewayService;

    @MockBean
    private EdgeStoreAndForwardSyncService syncService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private TenantContextFilter tenantContextFilter;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private UserRepository userRepository;

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
    @DisplayName("GET /api/v2/edge/gateways: returns list of edge gateways for Admin")
    void testGetGatewaysForAdmin() throws Exception {
        EdgeGatewayDto gateway = new EdgeGatewayDto();
        gateway.setId(UUID.randomUUID());
        gateway.setGatewayCode("EDGE-GW-AUSTIN-01");
        gateway.setName("Austin Primary Gateway");
        gateway.setStatus("ONLINE");
        gateway.setFirmwareVersion("2.0.0-EDGE");

        when(gatewayService.getAllGateways()).thenReturn(List.of(gateway));

        mockMvc.perform(get("/api/v2/edge/gateways")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].gatewayCode").value("EDGE-GW-AUSTIN-01"))
                .andExpect(jsonPath("$.data[0].status").value("ONLINE"));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("GET /api/v2/edge/gateways/{code}: returns gateway for Technician")
    void testGetGatewayByCodeForTechnician() throws Exception {
        EdgeGatewayDto gateway = new EdgeGatewayDto();
        gateway.setId(UUID.randomUUID());
        gateway.setGatewayCode("EDGE-GW-BERLIN-02");
        gateway.setName("Berlin Edge Node");
        gateway.setStatus("ONLINE");

        when(gatewayService.getGatewayByCode("EDGE-GW-BERLIN-02")).thenReturn(gateway);

        mockMvc.perform(get("/api/v2/edge/gateways/EDGE-GW-BERLIN-02")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gatewayCode").value("EDGE-GW-BERLIN-02"));
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    @DisplayName("POST /api/v2/edge/sync/batch: processes batch for Engineer")
    void testProcessSyncBatchForEngineer() throws Exception {
        EdgeSyncBatchResultDto result = new EdgeSyncBatchResultDto();
        result.setBatchId("BATCH-2026-001");
        result.setGatewayCode("EDGE-GW-AUSTIN-01");
        result.setSyncStatus(SyncStatus.RECONCILED);
        result.setTotalRecords(10);
        result.setProcessedRecords(10);
        result.setReconciledAt(Instant.now());

        when(syncService.processSyncBatch(any())).thenReturn(result);

        String payload = """
                {
                    "gatewayCode": "EDGE-GW-AUSTIN-01",
                    "batchId": "BATCH-2026-001",
                    "plantId": "00000000-0000-0000-0000-000000000201",
                    "sequenceStart": 1,
                    "sequenceEnd": 10,
                    "disconnectedAt": "2026-09-12T00:00:00Z",
                    "reconnectedAt": "2026-09-12T04:00:00Z",
                    "transactions": [
                        {
                            "sequenceId": 1,
                            "idempotencyKey": "KEY-1",
                            "transactionType": "PRODUCTION_OUTPUT",
                            "entityType": "ProductionOrder",
                            "payloadJson": "{\\"deltaGood\\": 10, \\"deltaScrap\\": 0}",
                            "recordedAt": "2026-09-12T01:00:00Z"
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/v2/edge/sync/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchId").value("BATCH-2026-001"))
                .andExpect(jsonPath("$.data.syncStatus").value("RECONCILED"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("POST /api/v2/edge/simulate-disconnect: denied for Operator")
    void testSimulateDisconnectDeniedForOperator() throws Exception {
        mockMvc.perform(post("/api/v2/edge/simulate-disconnect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gatewayCode\":\"EDGE-GW-AUSTIN-01\"}"))
                .andExpect(status().isForbidden());
    }
}
