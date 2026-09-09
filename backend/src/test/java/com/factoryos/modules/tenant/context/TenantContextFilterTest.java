package com.factoryos.modules.tenant.context;

import com.factoryos.modules.auth.domain.Role;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.domain.UserPlantMembership;
import com.factoryos.modules.tenant.repository.PlantRepository;
import com.factoryos.modules.tenant.repository.UserPlantMembershipRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantContextFilterTest {

    @Mock
    private PlantRepository plantRepository;

    @Mock
    private UserPlantMembershipRepository membershipRepository;

    @Mock
    private FilterChain filterChain;

    private TenantContextFilter filter;

    private User operatorUser;
    private Plant austinPlant;
    private Plant berlinPlant;
    private Role operatorRole;

    @BeforeEach
    void setUp() {
        filter = new TenantContextFilter(plantRepository, membershipRepository);

        operatorRole = new Role();
        operatorRole.setId(UUID.randomUUID());
        operatorRole.setName(RoleType.OPERATOR);

        operatorUser = new User();
        operatorUser.setId(UUID.randomUUID());
        operatorUser.setEmail("operator@austin.factoryos.internal");
        operatorUser.setDisplayName("Austin Operator");
        operatorUser.setRole(operatorRole);

        austinPlant = new Plant();
        austinPlant.setId(UUID.fromString("00000000-0000-0000-0000-000000000201"));
        austinPlant.setCode("PLANT-AUSTIN-01");
        austinPlant.setName("Austin Gigafactory");

        berlinPlant = new Plant();
        berlinPlant.setId(UUID.fromString("00000000-0000-0000-0000-000000000202"));
        berlinPlant.setCode("PLANT-BERLIN-02");
        berlinPlant.setName("Berlin Advanced Manufacturing");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void shouldResolveDefaultPlantWhenNoHeaderProvided() throws ServletException, IOException {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                operatorUser, null, List.of(new SimpleGrantedAuthority("ROLE_OPERATOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserPlantMembership membership = new UserPlantMembership(UUID.randomUUID(), operatorUser, austinPlant, operatorRole, true);
        when(membershipRepository.findByUserId(operatorUser.getId())).thenReturn(List.of(membership));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            TenantContext ctx = TenantContextHolder.getContext().orElse(null);
            assertNotNull(ctx);
            assertEquals(austinPlant.getId(), ctx.currentPlantId());
            assertEquals("PLANT-AUSTIN-01", ctx.currentPlantCode());
            assertFalse(ctx.isGlobalAdmin());
        });

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    void shouldAllowAccessToAuthorizedPlantViaHeader() throws ServletException, IOException {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                operatorUser, null, List.of(new SimpleGrantedAuthority("ROLE_OPERATOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserPlantMembership membership = new UserPlantMembership(UUID.randomUUID(), operatorUser, austinPlant, operatorRole, true);
        when(membershipRepository.findByUserId(operatorUser.getId())).thenReturn(List.of(membership));
        when(plantRepository.findByIdAndIsDeletedFalse(austinPlant.getId())).thenReturn(Optional.of(austinPlant));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Plant-ID", austinPlant.getId().toString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            TenantContext ctx = TenantContextHolder.getContext().orElse(null);
            assertNotNull(ctx);
            assertEquals(austinPlant.getId(), ctx.currentPlantId());
        });

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    void shouldRejectAccessToUnauthorizedPlantWithForbidden() throws ServletException, IOException {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                operatorUser, null, List.of(new SimpleGrantedAuthority("ROLE_OPERATOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Operator only has Austin membership
        UserPlantMembership membership = new UserPlantMembership(UUID.randomUUID(), operatorUser, austinPlant, operatorRole, true);
        when(membershipRepository.findByUserId(operatorUser.getId())).thenReturn(List.of(membership));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Plant-ID", berlinPlant.getId().toString()); // Requests Berlin!
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }
}
