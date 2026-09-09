package com.factoryos.modules.tenant.context;

import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.domain.UserPlantMembership;
import com.factoryos.modules.tenant.repository.PlantRepository;
import com.factoryos.modules.tenant.repository.UserPlantMembershipRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String PLANT_ID_HEADER = "X-Plant-ID";
    public static final String PLANT_CODE_HEADER = "X-Plant-Code";
    public static final UUID DEFAULT_AUSTIN_PLANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

    private final PlantRepository plantRepository;
    private final UserPlantMembershipRepository membershipRepository;

    public TenantContextFilter(PlantRepository plantRepository, UserPlantMembershipRepository membershipRepository) {
        this.plantRepository = plantRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User user) {
                boolean isGlobalAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName().name());
                List<UserPlantMembership> memberships = membershipRepository.findByUserId(user.getId());
                List<UUID> authorizedPlantIds = memberships.stream()
                        .map(m -> m.getPlant().getId())
                        .toList();

                String plantIdHeader = request.getHeader(PLANT_ID_HEADER);
                String plantCodeHeader = request.getHeader(PLANT_CODE_HEADER);

                UUID requestedPlantId = null;
                if (plantIdHeader != null && !plantIdHeader.isBlank()) {
                    try {
                        requestedPlantId = UUID.fromString(plantIdHeader.trim());
                    } catch (IllegalArgumentException ignored) {
                    }
                } else if (plantCodeHeader != null && !plantCodeHeader.isBlank()) {
                    Optional<Plant> p = plantRepository.findByCodeIgnoreCaseAndIsDeletedFalse(plantCodeHeader.trim());
                    if (p.isPresent()) {
                        requestedPlantId = p.get().getId();
                    }
                }

                Plant activePlant = null;
                String activePlantRole = user.getRole().getName().name();

                if (requestedPlantId != null) {
                    final UUID targetPlantId = requestedPlantId;
                    // Validate authorization
                    if (!isGlobalAdmin && !authorizedPlantIds.contains(targetPlantId)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cross-tenant access violation: user not authorized for plant " + targetPlantId);
                        return;
                    }
                    Optional<Plant> plantOpt = plantRepository.findByIdAndIsDeletedFalse(targetPlantId);
                    if (plantOpt.isPresent()) {
                        activePlant = plantOpt.get();
                        Optional<UserPlantMembership> mem = memberships.stream()
                                .filter(m -> m.getPlant().getId().equals(targetPlantId))
                                .findFirst();
                        if (mem.isPresent() && mem.get().getRole() != null) {
                            activePlantRole = mem.get().getRole().getName().name();
                        }
                    }
                }

                // If no requested plant or not found, fall back to user's default membership or Austin-01
                if (activePlant == null) {
                    Optional<UserPlantMembership> defaultMem = memberships.stream()
                            .filter(UserPlantMembership::isDefault)
                            .findFirst();
                    if (defaultMem.isPresent()) {
                        activePlant = defaultMem.get().getPlant();
                        activePlantRole = defaultMem.get().getRole().getName().name();
                    } else if (!memberships.isEmpty()) {
                        activePlant = memberships.get(0).getPlant();
                        activePlantRole = memberships.get(0).getRole().getName().name();
                    } else {
                        // Fallback to default Austin plant
                        activePlant = plantRepository.findByIdAndIsDeletedFalse(DEFAULT_AUSTIN_PLANT_ID)
                                .orElse(null);
                    }
                }

                if (activePlant != null) {
                    TenantContext ctx = new TenantContext(
                            activePlant.getEnterprise() != null ? activePlant.getEnterprise().getId() : null,
                            activePlant.getId(),
                            activePlant.getCode(),
                            activePlant.getName(),
                            activePlantRole,
                            isGlobalAdmin,
                            authorizedPlantIds
                    );
                    TenantContextHolder.setContext(ctx);
                    MDC.put("plantId", activePlant.getId().toString());
                    MDC.put("plantCode", activePlant.getCode());
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
