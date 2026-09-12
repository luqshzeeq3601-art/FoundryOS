package com.factoryos.modules.analytics.application;

import com.factoryos.modules.analytics.dto.EnterpriseOeeMatrixDto;
import com.factoryos.modules.analytics.dto.FleetSummaryDto;
import com.factoryos.modules.analytics.dto.PlantOeeBenchmarkDto;
import com.factoryos.modules.analytics.dto.ScheduledReportResponseDto;
import com.factoryos.modules.auth.infrastructure.JwtAuthenticationFilter;
import com.factoryos.modules.auth.infrastructure.JwtTokenService;
import com.factoryos.modules.auth.infrastructure.RateLimitingFilter;
import com.factoryos.modules.auth.infrastructure.SecurityConfig;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.tenant.context.TenantContextFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EnterpriseAnalyticsController.class)
@Import(SecurityConfig.class)
class EnterpriseAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnterpriseAnalyticsService analyticsService;

    @MockBean
    private EnterpriseExportService exportService;

    @MockBean
    private ScheduledEnterpriseReportService scheduledReportService;

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

    private EnterpriseOeeMatrixDto sampleMatrix;

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

        FleetSummaryDto fleet = new FleetSummaryDto(
                1, 1, 5, 4, 1, 0,
                85.0, 92.0, 95.0, 97.0,
                1000, 30, 2.9, 30
        );

        PlantOeeBenchmarkDto plant = new PlantOeeBenchmarkDto();
        plant.setPlantId(UUID.randomUUID());
        plant.setPlantCode("PLANT-AUSTIN-01");
        plant.setPlantName("Austin Gigafactory");
        plant.setRank(1);
        plant.setOee(85.0);
        plant.setBenchmarkTier("WORLD_CLASS");

        sampleMatrix = new EnterpriseOeeMatrixDto(
                UUID.randomUUID(),
                "ENT-GLOBAL",
                "FactoryOS Global Enterprise",
                "24H",
                Instant.now(),
                fleet,
                List.of(plant)
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnEnterpriseOeeMatrixForAdmin() throws Exception {
        when(analyticsService.getEnterpriseOeeMatrix(eq("24H"), any(), any())).thenReturn(sampleMatrix);

        mockMvc.perform(get("/api/v2/analytics/enterprise/oee-matrix")
                        .param("interval", "24H")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enterpriseCode").value("ENT-GLOBAL"))
                .andExpect(jsonPath("$.data.fleetSummary.fleetAvgOee").value(85.0))
                .andExpect(jsonPath("$.data.plantMetrics[0].plantCode").value("PLANT-AUSTIN-01"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void shouldReturnEnterpriseOeeMatrixForViewer() throws Exception {
        when(analyticsService.getEnterpriseOeeMatrix(eq("7D"), any(), any())).thenReturn(sampleMatrix);

        mockMvc.perform(get("/api/v2/analytics/enterprise/oee-matrix")
                        .param("interval", "7D")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enterpriseCode").value("ENT-GLOBAL"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldDenyAccessToEnterpriseOeeMatrixForOperator() throws Exception {
        mockMvc.perform(get("/api/v2/analytics/enterprise/oee-matrix")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PRODUCTION_MANAGER")
    void shouldExportCsvReport() throws Exception {
        when(analyticsService.getEnterpriseOeeMatrix(any(), any(), any())).thenReturn(sampleMatrix);
        when(exportService.generateCsvReport(any())).thenReturn("col1,col2\nval1,val2".getBytes());

        mockMvc.perform(get("/api/v2/analytics/enterprise/export/csv")
                        .param("interval", "24H"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/csv")))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    void shouldExportPdfReport() throws Exception {
        when(analyticsService.getEnterpriseOeeMatrix(any(), any(), any())).thenReturn(sampleMatrix);
        when(exportService.generatePdfReport(any())).thenReturn("%PDF-1.4 sample".getBytes());

        mockMvc.perform(get("/api/v2/analytics/enterprise/export/pdf")
                        .param("interval", "24H"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldTriggerScheduledReport() throws Exception {
        ScheduledReportResponseDto reportResponse = new ScheduledReportResponseDto(
                UUID.randomUUID(),
                "ENTERPRISE_OEE_EXECUTIVE_SUMMARY",
                "PDF",
                "GENERATED",
                1,
                85.0,
                2048,
                Instant.now(),
                "Report generated successfully"
        );

        when(scheduledReportService.generateAndAuditReport(eq("PDF"), eq("24H"), any())).thenReturn(reportResponse);

        mockMvc.perform(post("/api/v2/analytics/enterprise/reports/scheduled")
                        .param("format", "PDF")
                        .param("interval", "24H"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("GENERATED"))
                .andExpect(jsonPath("$.data.plantCount").value(1));
    }
}
