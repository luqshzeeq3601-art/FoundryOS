package com.factoryos.modules.barcode.application;

import com.factoryos.modules.auth.domain.Role;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.infrastructure.JwtAuthenticationFilter;
import com.factoryos.modules.auth.infrastructure.JwtTokenService;
import com.factoryos.modules.auth.infrastructure.RateLimitingFilter;
import com.factoryos.modules.auth.infrastructure.SecurityConfig;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.barcode.domain.BarcodeType;
import com.factoryos.modules.barcode.domain.BarcodeValidationStatus;
import com.factoryos.modules.barcode.domain.ResolvedEntityType;
import com.factoryos.modules.barcode.dto.BarcodeScanLogDto;
import com.factoryos.modules.barcode.dto.BarcodeScanRequest;
import com.factoryos.modules.barcode.dto.BarcodeScanResponse;
import com.factoryos.modules.barcode.dto.BomItemDto;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BarcodeController.class)
@Import(SecurityConfig.class)
class BarcodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BarcodeScanningService barcodeScanningService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private UserRepository userRepository;

    private User testOperator;

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

        Role role = new Role();
        role.setName(RoleType.OPERATOR);

        testOperator = new User();
        testOperator.setId(UUID.randomUUID());
        testOperator.setEmail("operator@factoryos.local");
        testOperator.setDisplayName("John Operator");
        testOperator.setRole(role);
        testOperator.setActive(true);

        when(userRepository.findByEmailIgnoreCaseAndIsDeletedFalse("operator@factoryos.local")).thenReturn(Optional.of(testOperator));
    }

    @Test
    @WithMockUser(username = "operator@factoryos.local", roles = {"OPERATOR"})
    void testScanBarcode_Success() throws Exception {
        BarcodeScanRequest request = new BarcodeScanRequest("ORD:ORD-2026-001", "QR_CODE", "HARDWARE_WEDGE", UUID.randomUUID(), null);
        BarcodeScanResponse mockResponse = BarcodeScanResponse.valid(
                UUID.randomUUID(), "ORD:ORD-2026-001", BarcodeType.TRAVELER, ResolvedEntityType.PRODUCTION_ORDER,
                UUID.randomUUID().toString(), "Order: ORD-2026-001", true, "Valid order", 12L, null);

        when(barcodeScanningService.processScan(any(BarcodeScanRequest.class), any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v2/barcode/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.barcodeType").value("TRAVELER"))
                .andExpect(jsonPath("$.data.validationStatus").value("VALID"))
                .andExpect(jsonPath("$.data.bomMatched").value(true));
    }

    @Test
    @WithMockUser(username = "operator@factoryos.local", roles = {"OPERATOR"})
    void testGetRecentScanLogs() throws Exception {
        BarcodeScanLogDto logDto = new BarcodeScanLogDto();
        logDto.setId(UUID.randomUUID());
        logDto.setScanPayload("LOT:LOT-ALU-6061-001");
        logDto.setBarcodeType(BarcodeType.MATERIAL_LOT);
        logDto.setValidationStatus(BarcodeValidationStatus.VALID);
        logDto.setCreatedAt(Instant.now());

        when(barcodeScanningService.getRecentScanLogs()).thenReturn(List.of(logDto));

        mockMvc.perform(get("/api/v2/barcode/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scanPayload").value("LOT:LOT-ALU-6061-001"))
                .andExpect(jsonPath("$.data[0].barcodeType").value("MATERIAL_LOT"));
    }

    @Test
    @WithMockUser(username = "operator@factoryos.local", roles = {"OPERATOR"})
    void testGetBomForProduct() throws Exception {
        BomItemDto bomDto = new BomItemDto(UUID.randomUUID(), "BRACKET-01", "RAW-ALU-6061", "Aluminum 6061", BigDecimal.valueOf(1.25), "KG");
        when(barcodeScanningService.getBomForProduct("BRACKET-01")).thenReturn(List.of(bomDto));

        mockMvc.perform(get("/api/v2/barcode/bom/BRACKET-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productCode").value("BRACKET-01"))
                .andExpect(jsonPath("$.data[0].materialCode").value("RAW-ALU-6061"));
    }
}
