package com.factoryos.modules.downtime.application;

import com.factoryos.modules.auth.infrastructure.JwtAuthenticationFilter;
import com.factoryos.modules.auth.infrastructure.JwtTokenService;
import com.factoryos.modules.auth.infrastructure.RateLimitingFilter;
import com.factoryos.modules.auth.infrastructure.SecurityConfig;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.downtime.domain.DowntimeReasonCode;
import com.factoryos.modules.downtime.domain.DowntimeTriggerSource;
import com.factoryos.modules.downtime.dto.AcknowledgeRootCauseRequest;
import com.factoryos.modules.downtime.dto.AutomatedEvaluationResultDto;
import com.factoryos.modules.downtime.dto.DowntimeEventDto;
import com.factoryos.modules.downtime.dto.MicroStopSummaryDto;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AutomatedDowntimeController.class)
@Import(SecurityConfig.class)
class AutomatedDowntimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AutomatedDowntimeDetectionService automatedDowntimeDetectionService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

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
    }

    @Test
    @DisplayName("GET /api/v2/downtime/pending-root-causes - should return pending prompt events")
    @WithMockUser(roles = "OPERATOR")
    void testGetPendingRootCauses() throws Exception {
        DowntimeEventDto dto = new DowntimeEventDto();
        dto.setId(UUID.randomUUID());
        dto.setMachineId(UUID.randomUUID());
        dto.setMachineName("CNC Lathe #1");
        dto.setReasonCode(DowntimeReasonCode.MICRO_STOP);
        dto.setTriggerSource(DowntimeTriggerSource.AUTOMATED_SENSOR);
        dto.setStartTime(Instant.now().minusSeconds(300));
        dto.setRootCausePromptedAt(Instant.now().minusSeconds(120));

        when(automatedDowntimeDetectionService.getPendingRootCauseEvents()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v2/downtime/pending-root-causes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].machineName").value("CNC Lathe #1"))
                .andExpect(jsonPath("$.data[0].reasonCode").value("MICRO_STOP"));
    }

    @Test
    @DisplayName("POST /api/v2/downtime/{id}/acknowledge-root-cause - should allow Operator to acknowledge")
    @WithMockUser(roles = "OPERATOR")
    void testAcknowledgeRootCauseByOperator() throws Exception {
        UUID eventId = UUID.randomUUID();
        DowntimeEventDto dto = new DowntimeEventDto();
        dto.setId(eventId);
        dto.setReasonCode(DowntimeReasonCode.TOOLING_JAM);
        dto.setDescription("Spindle tooling jammed on bar feeder");

        when(automatedDowntimeDetectionService.acknowledgeRootCause(eq(eventId), any(), any()))
                .thenReturn(dto);

        String json = """
                {
                    "reasonCode": "TOOLING_JAM",
                    "description": "Spindle tooling jammed on bar feeder",
                    "expectedVersion": 0
                }
                """;

        mockMvc.perform(post("/api/v2/downtime/{id}/acknowledge-root-cause", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reasonCode").value("TOOLING_JAM"))
                .andExpect(jsonPath("$.data.description").value("Spindle tooling jammed on bar feeder"));
    }

    @Test
    @DisplayName("GET /api/v2/downtime/micro-stops - should return aggregate summary")
    @WithMockUser(roles = "ENGINEER")
    void testGetMicroStopSummary() throws Exception {
        UUID machineId = UUID.randomUUID();
        MicroStopSummaryDto summary = new MicroStopSummaryDto(
                machineId,
                "Milling Cell 4",
                12,
                720,
                2,
                3600
        );

        when(automatedDowntimeDetectionService.getMicroStopSummary(eq(machineId), any(), any()))
                .thenReturn(summary);

        mockMvc.perform(get("/api/v2/downtime/micro-stops").param("machineId", machineId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.microStopCount").value(12))
                .andExpect(jsonPath("$.data.totalMicroStopSeconds").value(720))
                .andExpect(jsonPath("$.data.majorDowntimeCount").value(2));
    }

    @Test
    @DisplayName("POST /api/v2/downtime/evaluate/{machineId} - should evaluate stream and return state change")
    @WithMockUser(roles = "ENGINEER")
    void testEvaluateStream() throws Exception {
        UUID machineId = UUID.randomUUID();
        AutomatedEvaluationResultDto result = new AutomatedEvaluationResultDto(
                machineId,
                "TRIGGERED_DOWN",
                UUID.randomUUID(),
                "Machine entered DOWN status"
        );

        when(automatedDowntimeDetectionService.evaluateMachineStream(eq(machineId), eq(0.0), eq(0.0), any()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v2/downtime/evaluate/{machineId}", machineId)
                        .param("spindleSpeed", "0.0")
                        .param("cycleCountDelta", "0.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actionTaken").value("TRIGGERED_DOWN"));
    }
}
