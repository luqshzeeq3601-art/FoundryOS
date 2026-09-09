package com.factoryos.modules.telemetry.application;

import com.factoryos.modules.auth.infrastructure.JwtAuthenticationFilter;
import com.factoryos.modules.auth.infrastructure.JwtTokenService;
import com.factoryos.modules.auth.infrastructure.RateLimitingFilter;
import com.factoryos.modules.auth.infrastructure.SecurityConfig;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.telemetry.domain.ProtocolType;
import com.factoryos.modules.telemetry.domain.TelemetryQuality;
import com.factoryos.modules.telemetry.dto.*;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TelemetryController.class)
@Import(SecurityConfig.class)
class TelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TelemetryIngestionService telemetryService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setUpFilters() throws Exception {
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
    @DisplayName("Engineer can successfully ingest telemetry batch")
    @WithMockUser(username = "engineer@factoryos.local", roles = {"ENGINEER"})
    void testIngestTelemetryBatchAsEngineer() throws Exception {
        UUID machineId = UUID.randomUUID();
        TelemetryIngestResponse response = new TelemetryIngestResponse(machineId, 2, "SUCCESS", Collections.emptyList());
        when(telemetryService.ingestBatch(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v2/telemetry/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "machineId": "%s",
                            "gatewayId": "GW-01",
                            "points": [
                                {"tagName": "SPINDLE_SPEED", "value": 9000.0, "unit": "RPM"},
                                {"tagName": "VIBRATION_RMS", "value": 1.2, "unit": "mm/s"}
                            ]
                        }
                        """.formatted(machineId)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Operator is forbidden from ingesting telemetry batch")
    @WithMockUser(username = "operator@factoryos.local", roles = {"OPERATOR"})
    void testIngestTelemetryBatchAsOperatorForbidden() throws Exception {
        UUID machineId = UUID.randomUUID();

        mockMvc.perform(post("/api/v2/telemetry/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "machineId": "%s",
                            "gatewayId": "GW-01",
                            "points": [
                                {"tagName": "SPINDLE_SPEED", "value": 9000.0, "unit": "RPM"}
                            ]
                        }
                        """.formatted(machineId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Operator can view live machine telemetry")
    @WithMockUser(username = "operator@factoryos.local", roles = {"OPERATOR"})
    void testGetLiveTelemetryAsOperator() throws Exception {
        UUID machineId = UUID.randomUUID();
        MachineLiveTelemetryDto dto = new MachineLiveTelemetryDto();
        dto.setMachineId(machineId);
        dto.setHealthScore(95);
        dto.setConnectionStatus("ONLINE");
        when(telemetryService.getLiveTelemetry(eq(machineId))).thenReturn(dto);

        mockMvc.perform(get("/api/v2/telemetry/machines/{machineId}/live", machineId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin can register tag mapping")
    @WithMockUser(username = "admin@factoryos.local", roles = {"ADMIN"})
    void testCreateTagMappingAsAdmin() throws Exception {
        UUID machineId = UUID.randomUUID();
        TagMappingDto dto = new TagMappingDto();
        dto.setId(UUID.randomUUID());
        dto.setTagName("MOTOR_CURRENT");
        when(telemetryService.createTagMapping(eq(machineId), any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v2/telemetry/machines/{machineId}/tags", machineId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "tagName": "MOTOR_CURRENT",
                            "protocol": "OPC_UA",
                            "tagAddress": "ns=2;s=Device.Current",
                            "unitOfMeasure": "A"
                        }
                        """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Engineer cannot delete tag mapping (Admin only)")
    @WithMockUser(username = "engineer@factoryos.local", roles = {"ENGINEER"})
    void testDeleteTagMappingAsEngineerForbidden() throws Exception {
        UUID machineId = UUID.randomUUID();
        UUID mappingId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v2/telemetry/machines/{machineId}/tags/{mappingId}", machineId, mappingId))
                .andExpect(status().isForbidden());
    }
}
