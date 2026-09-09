package com.factoryos.modules.machine.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.common.dto.PagedResponse;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.dto.CreateMachineRequest;
import com.factoryos.modules.machine.dto.MachineDto;
import com.factoryos.modules.machine.dto.UpdateMachineRequest;
import com.factoryos.modules.machine.dto.UpdateMachineStatusRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/machines")
public class MachineController {

    private final MachineService machineService;

    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<PagedResponse<MachineDto>>> getMachines(
            @RequestParam(required = false) MachineStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(machineService.getMachines(status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<MachineDto>> getMachineById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(machineService.getMachineById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER')")
    public ResponseEntity<ApiResponse<MachineDto>> createMachine(
            @Valid @RequestBody CreateMachineRequest request,
            @AuthenticationPrincipal User actor
    ) {
        MachineDto dto = machineService.createMachine(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER')")
    public ResponseEntity<ApiResponse<MachineDto>> updateMachine(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMachineRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(machineService.updateMachine(id, request, actor)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<MachineDto>> updateMachineStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMachineStatusRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(machineService.updateMachineStatus(id, request, actor)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MachineDto>> archiveMachine(
            @PathVariable UUID id,
            @RequestParam Long expectedVersion,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(machineService.archiveMachine(id, expectedVersion, actor)));
    }
}
