package com.factoryos.modules.auth.api;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.common.dto.PagedResponse;
import com.factoryos.modules.auth.application.UserService;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.dto.CreateUserRequest;
import com.factoryos.modules.auth.dto.ResetPasswordRequest;
import com.factoryos.modules.auth.dto.UpdateUserRequest;
import com.factoryos.modules.auth.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getUsers(
            @RequestParam(required = false) RoleType role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(page, boundedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        PagedResponse<UserDto> response = userService.getUsers(role, isActive, search, pageRequest);
        return ResponseEntity.ok(ApiResponse.ok(response, "Users retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable UUID id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(user, "User details"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        UserDto created = userService.createUser(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created, "User created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        UserDto updated = userService.updateUser(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(updated, "User updated successfully"));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody ResetPasswordRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        userService.resetPassword(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(null, "Password reset successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> archiveUser(
            @PathVariable UUID id,
            @RequestParam Long expectedVersion,
            @AuthenticationPrincipal User currentUser
    ) {
        UserDto archived = userService.archiveUser(id, expectedVersion, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(archived, "User archived successfully"));
    }
}
