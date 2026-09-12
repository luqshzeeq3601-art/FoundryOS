package com.factoryos.modules.vibration.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.factoryos.modules.auth.infrastructure.JwtAuthenticationFilter;
import com.factoryos.modules.auth.infrastructure.JwtTokenService;
import com.factoryos.modules.auth.infrastructure.RateLimitingFilter;
import com.factoryos.modules.auth.infrastructure.SecurityConfig;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.tenant.context.TenantContextFilter;
import com.factoryos.modules.vibration.domain.FaultHarmonicType;
import com.factoryos.modules.vibration.domain.IsoSeverityZone;
import com.factoryos.modules.vibration.domain.MachineHealthStatus;
import com.factoryos.modules.vibration.dto.*;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VibrationAnalysisController.class)
@Import(SecurityConfig.class)
class VibrationAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MachineHealthScoringService scoringService;

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
    @WithMockUser(roles = "OPERATOR")
    void shouldReturnHealthAssessmentForOperator() throws Exception {
        UUID machineId = UUID.randomUUID();
        MachineHealthAssessmentDto dto = new MachineHealthAssessmentDto();
        dto.setMachineId(machineId);
        dto.setMachineName("CNC Line 1");
        dto.setHealthScore(92);
        dto.setHealthStatus(MachineHealthStatus.EXCELLENT);
        dto.setIsoSeverityZone(IsoSeverityZone.ZONE_A);
        dto.setRmsVelocityMmS(1.25);

        when(scoringService.getLatestAssessment(machineId)).thenReturn(dto);

        mockMvc.perform(get("/api/v2/vibration/machines/" + machineId + "/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.healthScore").value(92))
                .andExpect(jsonPath("$.data.healthStatus").value("EXCELLENT"))
                .andExpect(jsonPath("$.data.isoSeverityZone").value("ZONE_A"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void shouldReturnFleetSummaryForViewer() throws Exception {
        FleetHealthSummaryDto fleet = new FleetHealthSummaryDto();
        fleet.setTotalMachinesAssessed(5);
        fleet.setAverageFleetHealthScore(88.5);
        fleet.setZoneACount(4);
        fleet.setZoneBCount(1);

        when(scoringService.getFleetHealthSummary(any())).thenReturn(fleet);

        mockMvc.perform(get("/api/v2/vibration/fleet/health-summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMachinesAssessed").value(5))
                .andExpect(jsonPath("$.data.averageFleetHealthScore").value(88.5));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldDenySimulateBurstForOperator() throws Exception {
        SimulateBurstRequestDto req = new SimulateBurstRequestDto();
        req.setMachineId(UUID.randomUUID());
        req.setFaultType(FaultHarmonicType.NORMAL);

        mockMvc.perform(post("/api/v2/vibration/simulate-burst")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    void shouldAllowSimulateBurstForEngineer() throws Exception {
        UUID machineId = UUID.randomUUID();
        SimulateBurstRequestDto req = new SimulateBurstRequestDto();
        req.setMachineId(machineId);
        req.setFaultType(FaultHarmonicType.BPFO_BEARING_OUTER);

        MachineHealthAssessmentDto assessment = new MachineHealthAssessmentDto();
        assessment.setMachineId(machineId);
        assessment.setHealthScore(65);
        assessment.setHealthStatus(MachineHealthStatus.FAIR_DEGRADED);
        assessment.setDominantFaultType("BPFO_BEARING_OUTER");

        when(scoringService.simulateBurst(any(), any())).thenReturn(assessment);

        mockMvc.perform(post("/api/v2/vibration/simulate-burst")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.healthScore").value(65))
                .andExpect(jsonPath("$.data.dominantFaultType").value("BPFO_BEARING_OUTER"));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void shouldReturnSpectrumForTechnician() throws Exception {
        UUID machineId = UUID.randomUUID();
        FftSpectrumDto spectrum = new FftSpectrumDto();
        spectrum.setMachineId(machineId);
        spectrum.setSampleRateHz(2048.0);
        spectrum.setFundamentalFrequencyHz(50.0);
        spectrum.setRmsVelocityMmS(1.85);
        spectrum.setFrequencies(new double[]{0.0, 50.0, 100.0});
        spectrum.setAmplitudes(new double[]{0.01, 1.2, 0.3});

        when(scoringService.getLatestSpectrum(machineId)).thenReturn(spectrum);

        mockMvc.perform(get("/api/v2/vibration/machines/" + machineId + "/spectrum")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sampleRateHz").value(2048.0))
                .andExpect(jsonPath("$.data.fundamentalFrequencyHz").value(50.0));
    }
}
