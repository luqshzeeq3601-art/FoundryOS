package com.factoryos.modules.tenant.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.tenant.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/hierarchy")
public class HierarchyController {

    private final HierarchyService hierarchyService;

    public HierarchyController(HierarchyService hierarchyService) {
        this.hierarchyService = hierarchyService;
    }

    @GetMapping("/enterprises")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<EnterpriseDto>>> getEnterprises() {
        return ResponseEntity.ok(ApiResponse.ok(hierarchyService.getEnterprises()));
    }

    @GetMapping("/plants")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<PlantDto>>> getPlants(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(hierarchyService.getAuthorizedPlants(user)));
    }

    @GetMapping("/plants/{plantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<PlantDto>> getPlantById(@PathVariable UUID plantId) {
        return ResponseEntity.ok(ApiResponse.ok(hierarchyService.getPlantById(plantId)));
    }

    @GetMapping("/plants/{plantId}/tree")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<HierarchyTreeDto>> getPlantHierarchyTree(@PathVariable UUID plantId) {
        return ResponseEntity.ok(ApiResponse.ok(hierarchyService.getPlantHierarchyTree(plantId)));
    }

    @PostMapping("/plants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PlantDto>> createPlant(
            @Valid @RequestBody CreatePlantRequest request,
            @AuthenticationPrincipal User actor
    ) {
        PlantDto created = hierarchyService.createPlant(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @PostMapping("/areas")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER')")
    public ResponseEntity<ApiResponse<ProductionAreaDto>> createArea(
            @Valid @RequestBody CreateAreaRequest request,
            @AuthenticationPrincipal User actor
    ) {
        ProductionAreaDto created = hierarchyService.createArea(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @PostMapping("/lines")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER')")
    public ResponseEntity<ApiResponse<ProductionLineDto>> createLine(
            @Valid @RequestBody CreateLineRequest request,
            @AuthenticationPrincipal User actor
    ) {
        ProductionLineDto created = hierarchyService.createLine(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @PostMapping("/work-cells")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER')")
    public ResponseEntity<ApiResponse<WorkCellDto>> createWorkCell(
            @Valid @RequestBody CreateWorkCellRequest request,
            @AuthenticationPrincipal User actor
    ) {
        WorkCellDto created = hierarchyService.createWorkCell(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @GetMapping("/users/{userId}/memberships")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<List<UserPlantMembershipDto>>> getUserMemberships(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(hierarchyService.getUserMemberships(userId)));
    }

    @PostMapping("/users/{userId}/memberships")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserPlantMembershipDto>> assignPlantMembership(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignPlantMembershipRequest request,
            @AuthenticationPrincipal User actor
    ) {
        UserPlantMembershipDto assigned = hierarchyService.assignPlantMembership(userId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(assigned));
    }

    @DeleteMapping("/users/{userId}/memberships/{plantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removePlantMembership(
            @PathVariable UUID userId,
            @PathVariable UUID plantId,
            @AuthenticationPrincipal User actor
    ) {
        hierarchyService.removePlantMembership(userId, plantId, actor);
        return ResponseEntity.ok(ApiResponse.ok(null, "Plant membership removed successfully"));
    }
}
