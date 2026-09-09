package com.factoryos.modules.auth.application;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditRecordingService auditRecordingService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                roleRepository,
                refreshSessionRepository,
                passwordEncoder,
                auditRecordingService
        );
    }

    @Test
    void createUser_Success_NormalizesEmailAndRecordsAudit() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("  Operator.Lead@FactoryOS.local  ");
        request.setDisplayName("Operator Lead");
        request.setTemporaryPassword("TempPass123!Secure");
        request.setRole(RoleType.OPERATOR);

        Role role = new Role(RoleType.OPERATOR);
        when(userRepository.existsByEmailAndIsDeletedFalse("operator.lead@factoryos.local")).thenReturn(false);
        when(roleRepository.findByName(RoleType.OPERATOR)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("TempPass123!Secure")).thenReturn("hashedPassword");

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("operator.lead@factoryos.local");
        savedUser.setDisplayName("Operator Lead");
        savedUser.setRole(role);
        savedUser.setActive(true);
        savedUser.setMustChangePassword(true);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User actor = new User();
        actor.setId(UUID.randomUUID());

        UserDto result = userService.createUser(request, actor);

        assertNotNull(result);
        assertEquals("operator.lead@factoryos.local", result.getEmail());
        assertTrue(result.isMustChangePassword());
        verify(auditRecordingService).record(eq(actor.getId()), eq("USER_CREATED"), eq("User"), eq(savedUser.getId()), isNull(), anyMap());
    }

    @Test
    void createUser_DuplicateEmail_ThrowsConflict() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("admin@factoryos.local");
        request.setDisplayName("Admin");
        request.setTemporaryPassword("Password123!");
        request.setRole(RoleType.ADMIN);

        when(userRepository.existsByEmailAndIsDeletedFalse("admin@factoryos.local")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> userService.createUser(request, null));
        assertEquals("EMAIL_ALREADY_EXISTS", ex.getErrorCode());
    }

    @Test
    void updateUser_LastAdminProtection_ThrowsConflict() {
        UUID adminId = UUID.randomUUID();
        User adminUser = new User();
        adminUser.setId(adminId);
        adminUser.setEmail("admin@factoryos.local");
        adminUser.setDisplayName("Admin User");
        adminUser.setRole(new Role(RoleType.ADMIN));
        adminUser.setActive(true);
        adminUser.setVersion(0L);

        when(userRepository.findByIdAndIsDeletedFalse(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.countActiveUsersByRole(RoleType.ADMIN)).thenReturn(1L);

        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setIsActive(false); // Attempting to deactivate the only active admin
        updateRequest.setExpectedVersion(0L);

        AppException ex = assertThrows(AppException.class, () -> userService.updateUser(adminId, updateRequest, null));
        assertEquals("LAST_ADMIN_PROTECTION", ex.getErrorCode());
    }

    @Test
    void updateUser_OptimisticLocking_VersionConflict() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setVersion(2L);

        when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(user));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setExpectedVersion(1L); // Stale version

        AppException ex = assertThrows(AppException.class, () -> userService.updateUser(userId, request, null));
        assertEquals("VERSION_CONFLICT", ex.getErrorCode());
    }

    @Test
    void resetPassword_ForcesMustChangePasswordAndRevokesSessions() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("tech@factoryos.local");
        user.setRole(new Role(RoleType.TECHNICIAN));
        user.setVersion(0L);

        when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewTempPassword123!")).thenReturn("newHashed");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setTemporaryPassword("NewTempPassword123!");
        request.setExpectedVersion(0L);

        userService.resetPassword(userId, request, null);

        assertTrue(user.isMustChangePassword());
        verify(refreshSessionRepository).revokeAllForUser(eq(user), any(Instant.class));
        verify(auditRecordingService).record(isNull(), eq("USER_PASSWORD_RESET"), eq("User"), eq(userId), isNull(), anyMap());
    }
}
