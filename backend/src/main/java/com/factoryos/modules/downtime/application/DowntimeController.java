package com.factoryos.modules.downtime.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.common.dto.PagedResponse;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.downtime.dto.CreateDowntimeRequest;
import com.factoryos.modules.downtime.dto.DowntimeEventDto;
import com.factoryos.modules.downtime.dto.ResolveDowntimeRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/downtime-events")
public class DowntimeController {

    private final DowntimeService downtimeService;

    public DowntimeController(DowntimeService downtimeService) {
        this.downtimeService = downtimeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<PagedResponse<DowntimeEventDto>>> getDowntimeEvents(
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) Boolean openOnly,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toTime,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                downtimeService.getDowntimeEvents(machineId, openOnly, fromTime, toTime, pageable)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<DowntimeEventDto>> getDowntimeEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(downtimeService.getDowntimeEventById(id)));
    }

    @GetMapping("/active/machine/{machineId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<DowntimeEventDto>> getActiveDowntimeForMachine(@PathVariable UUID machineId) {
        Optional<DowntimeEventDto> active = downtimeService.getActiveDowntimeForMachine(machineId);
        return active.map(dto -> ResponseEntity.ok(ApiResponse.ok(dto)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(null)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<DowntimeEventDto>> createDowntimeEvent(
            @Valid @RequestBody CreateDowntimeRequest request,
            @AuthenticationPrincipal User actor
    ) {
        DowntimeEventDto dto = downtimeService.createDowntimeEvent(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(dto));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<DowntimeEventDto>> resolveDowntimeEvent(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveDowntimeRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(downtimeService.resolveDowntimeEvent(id, request, actor)));
    }
}
