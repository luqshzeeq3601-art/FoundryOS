package com.factoryos.modules.materials;

import com.factoryos.modules.auth.infrastructure.JwtAuthenticationFilter;
import com.factoryos.modules.auth.infrastructure.JwtTokenService;
import com.factoryos.modules.auth.infrastructure.RateLimitingFilter;
import com.factoryos.modules.auth.infrastructure.SecurityConfig;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.materials.application.MaterialBackflushingService;
import com.factoryos.modules.materials.application.MaterialController;
import com.factoryos.modules.materials.dto.BomExplosionDto;
import com.factoryos.modules.materials.dto.MaterialConsumptionRecordDto;
import com.factoryos.modules.materials.dto.MaterialDto;
import com.factoryos.modules.materials.dto.RecordProductionOutputRequest;
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

import java.math.BigDecimal;
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

@WebMvcTest(controllers = MaterialController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class MaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MaterialBackflushingService materialBackflushingService;

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
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("GET /api/v2/materials should return materials catalog")
    void testGetAllMaterials() throws Exception {
        MaterialDto m = new MaterialDto();
        m.setId(UUID.randomUUID());
        m.setMaterialCode("RAW-ALU-6061");
        m.setMaterialName("Aluminum 6061");
        m.setCurrentStock(BigDecimal.valueOf(2500.0));

        when(materialBackflushingService.getAllMaterials()).thenReturn(List.of(m));

        mockMvc.perform(get("/api/v2/materials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].materialCode").value("RAW-ALU-6061"));
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    @DisplayName("GET /api/v2/materials/bom/{productCode}/explosion should return BOM explosion")
    void testGetBomExplosion() throws Exception {
        BomExplosionDto dto = new BomExplosionDto("SPINDLE-ROT-V2", 50);
        dto.setAllMaterialsInStock(true);

        when(materialBackflushingService.getBomExplosion("SPINDLE-ROT-V2", 50)).thenReturn(dto);

        mockMvc.perform(get("/api/v2/materials/bom/SPINDLE-ROT-V2/explosion?plannedQuantity=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productCode").value("SPINDLE-ROT-V2"))
                .andExpect(jsonPath("$.data.allMaterialsInStock").value(true));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("POST /api/v2/materials/orders/{id}/record-output should record production and backflush")
    void testRecordProductionOutput() throws Exception {
        UUID orderId = UUID.randomUUID();
        MaterialConsumptionRecordDto rec = new MaterialConsumptionRecordDto();
        rec.setId(UUID.randomUUID());
        rec.setMaterialCode("RAW-ALU-6061");
        rec.setTheoreticalQuantity(BigDecimal.valueOf(50.0));
        rec.setActualQuantity(BigDecimal.valueOf(50.0));
        rec.setVariancePercentage(BigDecimal.ZERO);
        rec.setVarianceAlertTriggered(false);

        when(materialBackflushingService.recordProductionOutputAndBackflush(eq(orderId), any(RecordProductionOutputRequest.class), any()))
                .thenReturn(List.of(rec));

        mockMvc.perform(post("/api/v2/materials/orders/" + orderId + "/record-output")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"incrementalGoodQuantity\":20,\"incrementalScrapQuantity\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].materialCode").value("RAW-ALU-6061"))
                .andExpect(jsonPath("$.data[0].varianceAlertTriggered").value(false));
    }

    @Test
    @WithMockUser(roles = "PRODUCTION_MANAGER")
    @DisplayName("GET /api/v2/materials/variance-alerts should return variance alerts")
    void testGetVarianceAlerts() throws Exception {
        MaterialConsumptionRecordDto rec = new MaterialConsumptionRecordDto();
        rec.setId(UUID.randomUUID());
        rec.setMaterialCode("RAW-STL-4140");
        rec.setVariancePercentage(BigDecimal.valueOf(12.5));
        rec.setVarianceAlertTriggered(true);

        when(materialBackflushingService.getVarianceAlerts()).thenReturn(List.of(rec));

        mockMvc.perform(get("/api/v2/materials/variance-alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].materialCode").value("RAW-STL-4140"))
                .andExpect(jsonPath("$.data[0].varianceAlertTriggered").value(true));
    }
}
