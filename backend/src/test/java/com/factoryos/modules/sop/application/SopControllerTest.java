package com.factoryos.modules.sop.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.factoryos.modules.auth.infrastructure.JwtAuthenticationFilter;
import com.factoryos.modules.auth.infrastructure.JwtTokenService;
import com.factoryos.modules.auth.infrastructure.RateLimitingFilter;
import com.factoryos.modules.auth.infrastructure.SecurityConfig;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.sop.domain.*;
import com.factoryos.modules.sop.dto.*;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SopController.class)
@Import(SecurityConfig.class)
class SopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SopService sopService;

    @MockBean
    private QualityGateService qualityGateService;

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
    void shouldListSopsForOperator() throws Exception {
        SopDto sop = new SopDto();
        sop.setId(UUID.randomUUID());
        sop.setSopCode("SOP-TURBINE-001");
        sop.setTitle("Turbine Blade Machining");
        sop.setCategory(SopCategory.ASSEMBLY);

        when(sopService.getAllSops(any(), any())).thenReturn(List.of(sop));

        mockMvc.perform(get("/api/v2/sop/templates")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sopCode").value("SOP-TURBINE-001"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldDenySopCreationForOperator() throws Exception {
        SopCreateRequestDto req = new SopCreateRequestDto();
        req.setSopCode("SOP-NEW-01");
        req.setTitle("New SOP");
        req.setProductCode("TURBINE-01");

        mockMvc.perform(post("/api/v2/sop/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    void shouldAllowSopCreationForEngineer() throws Exception {
        SopCreateRequestDto req = new SopCreateRequestDto();
        req.setSopCode("SOP-NEW-01");
        req.setTitle("New SOP");
        req.setProductCode("TURBINE-01");

        SopDto created = new SopDto();
        created.setId(UUID.randomUUID());
        created.setSopCode("SOP-NEW-01");
        created.setTitle("New SOP");

        when(sopService.createSop(any(), any())).thenReturn(created);

        mockMvc.perform(post("/api/v2/sop/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sopCode").value("SOP-NEW-01"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldStartSessionForOperator() throws Exception {
        UUID orderId = UUID.randomUUID();
        StartSopSessionRequestDto req = new StartSopSessionRequestDto();
        req.setProductionOrderId(orderId);

        SopExecutionSessionDto sessionDto = new SopExecutionSessionDto();
        sessionDto.setId(UUID.randomUUID());
        sessionDto.setOrderNumber("PO-2026-001");
        sessionDto.setSessionStatus(SopSessionStatus.IN_PROGRESS);

        when(sopService.startExecutionSession(any(), any())).thenReturn(sessionDto);

        mockMvc.perform(post("/api/v2/sop/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderNumber").value("PO-2026-001"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldRecordStepExecution() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID stepId = UUID.randomUUID();
        RecordStepExecutionRequestDto req = new RecordStepExecutionRequestDto();
        req.setStepId(stepId);
        req.setStatus(StepRecordStatus.PASSED);
        req.setNumericValue(12.5);

        SopStepExecutionRecordDto record = new SopStepExecutionRecordDto();
        record.setId(UUID.randomUUID());
        record.setStatus(StepRecordStatus.PASSED);
        record.setIsWithinTolerance(true);

        when(sopService.recordStepExecution(eq(sessionId), any(), any())).thenReturn(record);

        mockMvc.perform(post("/api/v2/sop/sessions/" + sessionId + "/steps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PASSED"))
                .andExpect(jsonPath("$.data.isWithinTolerance").value(true));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void shouldGetQualityGateStatusForViewer() throws Exception {
        UUID orderId = UUID.randomUUID();
        QualityGateStatusDto gate = new QualityGateStatusDto();
        gate.setProductionOrderId(orderId);
        gate.setOrderNumber("PO-2026-001");
        gate.setGateStatus(GateStatus.PASSED);
        gate.setCompliant(true);

        when(qualityGateService.getGateStatusForOrder(orderId)).thenReturn(gate);

        mockMvc.perform(get("/api/v2/sop/gates/order/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gateStatus").value("PASSED"))
                .andExpect(jsonPath("$.data.compliant").value(true));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldDenySignOffGateForOperator() throws Exception {
        UUID orderId = UUID.randomUUID();
        QualitySignOffRequestDto req = new QualitySignOffRequestDto();
        req.setGateStatus(GateStatus.PASSED);
        req.setSignOffComments("Approved");

        mockMvc.perform(post("/api/v2/sop/gates/order/" + orderId + "/sign-off")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void shouldAllowSignOffGateForTechnician() throws Exception {
        UUID orderId = UUID.randomUUID();
        QualitySignOffRequestDto req = new QualitySignOffRequestDto();
        req.setGateStatus(GateStatus.PASSED);
        req.setSignOffComments("Quality stamp applied");

        QualityGateStatusDto gate = new QualityGateStatusDto();
        gate.setProductionOrderId(orderId);
        gate.setGateStatus(GateStatus.PASSED);
        gate.setCompliant(true);
        gate.setSignedOffByName("Tech User");

        when(qualityGateService.signOffGate(eq(orderId), any(), any())).thenReturn(gate);

        mockMvc.perform(post("/api/v2/sop/gates/order/" + orderId + "/sign-off")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gateStatus").value("PASSED"))
                .andExpect(jsonPath("$.data.signedOffByName").value("Tech User"));
    }
}
