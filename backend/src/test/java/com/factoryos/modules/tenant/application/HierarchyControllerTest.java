package com.factoryos.modules.tenant.application;

import com.factoryos.modules.auth.domain.Role;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.infrastructure.JwtAuthenticationFilter;
import com.factoryos.modules.auth.infrastructure.JwtTokenService;
import com.factoryos.modules.auth.infrastructure.RateLimitingFilter;
import com.factoryos.modules.auth.infrastructure.SecurityConfig;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.tenant.context.TenantContextFilter;
import com.factoryos.modules.tenant.dto.*;
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

@WebMvcTest(controllers = HierarchyController.class)
@Import(SecurityConfig.class)
class HierarchyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HierarchyService hierarchyService;

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

    private PlantDto austinPlantDto;

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

        austinPlantDto = new PlantDto(
                UUID.fromString("00000000-0000-0000-0000-000000000201"),
                UUID.fromString("00000000-0000-0000-0000-000000000100"),
                "FactoryOS Global Enterprise",
                "PLANT-AUSTIN-01",
                "Austin Gigafactory",
                "America/Chicago",
                "13101 Harold Green Rd, Austin, TX 78725",
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnAuthorizedPlants() throws Exception {
        when(hierarchyService.getAuthorizedPlants(any())).thenReturn(List.of(austinPlantDto));

        mockMvc.perform(get("/api/v2/hierarchy/plants")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("PLANT-AUSTIN-01"))
                .andExpect(jsonPath("$.data[0].name").value("Austin Gigafactory"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldReturnPlantHierarchyTree() throws Exception {
        HierarchyTreeDto tree = new HierarchyTreeDto(
                austinPlantDto.id(),
                austinPlantDto.code(),
                austinPlantDto.name(),
                austinPlantDto.timezone(),
                austinPlantDto.status(),
                List.of(
                        new HierarchyTreeDto.AreaNodeDto(
                                UUID.randomUUID(), "AREA-MACHINING", "Heavy Machining",
                                List.of(
                                        new HierarchyTreeDto.LineNodeDto(
                                                UUID.randomUUID(), "LINE-MILL-01", "CNC Milling Line",
                                                List.of()
                                        )
                                )
                        )
                )
        );

        when(hierarchyService.getPlantHierarchyTree(eq(austinPlantDto.id()))).thenReturn(tree);

        mockMvc.perform(get("/api/v2/hierarchy/plants/{plantId}/tree", austinPlantDto.id())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plantCode").value("PLANT-AUSTIN-01"))
                .andExpect(jsonPath("$.data.areas[0].areaCode").value("AREA-MACHINING"))
                .andExpect(jsonPath("$.data.areas[0].lines[0].lineCode").value("LINE-MILL-01"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreatePlant() throws Exception {
        CreatePlantRequest request = new CreatePlantRequest(
                UUID.fromString("00000000-0000-0000-0000-000000000100"),
                "PLANT-TOKYO-03",
                "Tokyo Advanced Robotics",
                "Asia/Tokyo",
                "Tokyo Factory Complex"
        );

        PlantDto createdDto = new PlantDto(
                UUID.randomUUID(),
                request.enterpriseId(),
                "FactoryOS Global Enterprise",
                request.code(),
                request.name(),
                request.timezone(),
                request.address(),
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        when(hierarchyService.createPlant(any(), any())).thenReturn(createdDto);

        mockMvc.perform(post("/api/v2/hierarchy/plants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("PLANT-TOKYO-03"));
    }
}
