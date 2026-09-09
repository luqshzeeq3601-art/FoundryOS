package com.factoryos.modules.auth.application;

import com.factoryos.common.dto.PagedResponse;
import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.Role;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.dto.CreateUserRequest;
import com.factoryos.modules.auth.dto.ResetPasswordRequest;
import com.factoryos.modules.auth.dto.UpdateUserRequest;
import com.factoryos.modules.auth.dto.UserDto;
import com.factoryos.modules.auth.repository.RefreshSessionRepository;
import com.factoryos.modules.auth.repository.RoleRepository;
import com.factoryos.modules.auth.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecordingService auditRecordingService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshSessionRepository refreshSessionRepository,
            PasswordEncoder passwordEncoder,
            AuditRecordingService auditRecordingService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditRecordingService = auditRecordingService;
    }

    public PagedResponse<UserDto> getUsers(RoleType role, Boolean isActive, String search, Pageable pageable) {
        Page<User> users = userRepository.searchUsers(role, isActive, search, pageable);
        return PagedResponse.from(users.map(UserDto::from));
    }

    public UserDto getUserById(UUID id) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("User not found"));
        return UserDto.from(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request, User actor) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailAndIsDeletedFalse(normalizedEmail)) {
            throw AppException.conflict("EMAIL_ALREADY_EXISTS", "A user with this email already exists");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> AppException.badRequest("Invalid role: " + request.getRole()));

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setDisplayName(request.getDisplayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getTemporaryPassword()));
        user.setRole(role);
        user.setActive(true);
        user.setMustChangePassword(true);
        user.setCreatedBy(actor != null ? actor.getId() : null);
        user.setUpdatedBy(actor != null ? actor.getId() : null);

        User saved = userRepository.save(user);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "USER_CREATED",
                "User",
                saved.getId(),
                null,
                Map.of("email", saved.getEmail(), "role", role.getName().name(), "displayName", saved.getDisplayName())
        );

        return UserDto.from(saved);
    }

    @Transactional
    public UserDto updateUser(UUID id, UpdateUserRequest request, User actor) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (request.getExpectedVersion() == null || !request.getExpectedVersion().equals(user.getVersion())) {
            throw AppException.versionConflict("User has been modified by another transaction. Reload and retry.");
        }

        Map<String, Object> beforeState = Map.of(
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : "",
                "role", user.getRole() != null ? user.getRole().getName().name() : "",
                "isActive", user.isActive()
        );

        // Check Last Active Admin invariant
        boolean isCurrentlyActiveAdmin = user.isActive() && user.getRole() != null && user.getRole().getName() == RoleType.ADMIN;
        boolean willBeDemotedOrDeactivated = 
                (request.getRole() != null && request.getRole() != RoleType.ADMIN) ||
                (request.getIsActive() != null && !request.getIsActive());

        if (isCurrentlyActiveAdmin && willBeDemotedOrDeactivated) {
            long activeAdminCount = userRepository.countActiveUsersByRole(RoleType.ADMIN);
            if (activeAdminCount <= 1) {
                throw AppException.conflict("LAST_ADMIN_PROTECTION", "Cannot demote or deactivate the final active Administrator.");
            }
        }

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setDisplayName(request.getDisplayName().trim());
        }

        if (request.getRole() != null) {
            Role role = roleRepository.findByName(request.getRole())
                    .orElseThrow(() -> AppException.badRequest("Invalid role: " + request.getRole()));
            user.setRole(role);
        }

        if (request.getIsActive() != null) {
            user.setActive(request.getIsActive());
            if (!request.getIsActive()) {
                refreshSessionRepository.revokeAllForUser(user, Instant.now());
            }
        }

        user.setUpdatedBy(actor != null ? actor.getId() : null);
        user.setUpdatedAt(Instant.now());

        User saved = userRepository.save(user);

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "USER_UPDATED",
                "User",
                saved.getId(),
                beforeState,
                Map.of("displayName", saved.getDisplayName() != null ? saved.getDisplayName() : "", "role", saved.getRole() != null ? saved.getRole().getName().name() : "", "isActive", saved.isActive())
        );

        return UserDto.from(saved);
    }

    @Transactional
    public void resetPassword(UUID id, ResetPasswordRequest request, User actor) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (request.getExpectedVersion() == null || !request.getExpectedVersion().equals(user.getVersion())) {
            throw AppException.versionConflict("User has been modified by another transaction. Reload and retry.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getTemporaryPassword()));
        user.setMustChangePassword(true);
        user.setUpdatedBy(actor != null ? actor.getId() : null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        refreshSessionRepository.revokeAllForUser(user, Instant.now());

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "USER_PASSWORD_RESET",
                "User",
                user.getId(),
                null,
                Map.of("email", user.getEmail(), "resetBy", actor != null ? actor.getEmail() : "SYSTEM")
        );
    }

    @Transactional
    public UserDto archiveUser(UUID id, Long expectedVersion, User actor) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (expectedVersion == null || !expectedVersion.equals(user.getVersion())) {
            throw AppException.versionConflict("User has been modified by another transaction. Reload and retry.");
        }

        if (user.isActive() && user.getRole().getName() == RoleType.ADMIN) {
            long activeAdminCount = userRepository.countActiveUsersByRole(RoleType.ADMIN);
            if (activeAdminCount <= 1) {
                throw AppException.conflict("LAST_ADMIN_PROTECTION", "Cannot archive the final active Administrator.");
            }
        }

        user.setDeleted(true);
        user.setActive(false);
        user.setUpdatedBy(actor != null ? actor.getId() : null);
        user.setUpdatedAt(Instant.now());

        User saved = userRepository.save(user);
        refreshSessionRepository.revokeAllForUser(saved, Instant.now());

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "USER_ARCHIVED",
                "User",
                saved.getId(),
                Map.of("isActive", true, "isDeleted", false),
                Map.of("isActive", false, "isDeleted", true)
        );

        return UserDto.from(saved);
    }
}
