package com.factoryos.modules.auth.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.auth.domain.RefreshSession;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.dto.ChangePasswordRequest;
import com.factoryos.modules.auth.dto.LoginRequest;
import com.factoryos.modules.auth.dto.LoginResponse;
import com.factoryos.modules.auth.dto.TokenRefreshResponse;
import com.factoryos.modules.auth.dto.UserDto;
import com.factoryos.modules.auth.infrastructure.JwtTokenService;
import com.factoryos.modules.auth.repository.RefreshSessionRepository;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.domain.UserPlantMembership;
import com.factoryos.modules.tenant.dto.UserPlantMembershipDto;
import com.factoryos.modules.tenant.repository.PlantRepository;
import com.factoryos.modules.tenant.repository.UserPlantMembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    public static final UUID DEFAULT_AUSTIN_PLANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final UserPlantMembershipRepository membershipRepository;
    private final PlantRepository plantRepository;

    public AuthService(
            UserRepository userRepository,
            RefreshSessionRepository refreshSessionRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            UserPlantMembershipRepository membershipRepository,
            PlantRepository plantRepository
    ) {
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.membershipRepository = membershipRepository;
        this.plantRepository = plantRepository;
    }

    public record LoginResult(LoginResponse response, String rawRefreshToken) {}

    @Transactional
    public LoginResult login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailAndIsDeletedFalse(normalizedEmail);

        if (userOpt.isEmpty()) {
            log.warn("Login attempt for non-existent email");
            throw AppException.unauthorized("Invalid email or password");
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            log.warn("Login attempt for inactive user {}", user.getId());
            throw AppException.unauthorized("Invalid email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password for user {}", user.getId());
            throw AppException.unauthorized("Invalid email or password");
        }

        // Resolve plant memberships
        UserDto userDto = buildEnrichedUserDto(user, null);

        String accessToken = jwtTokenService.generateAccessToken(
                user,
                userDto.getActivePlantId(),
                userDto.getActivePlantCode(),
                userDto.getPlantRole(),
                userDto.getAuthorizedPlants() != null ? userDto.getAuthorizedPlants().stream().map(UserPlantMembershipDto::plantCode).toList() : null
        );

        String rawRefreshToken = jwtTokenService.generateOpaqueRefreshToken();
        String tokenHash = jwtTokenService.hashToken(rawRefreshToken);

        RefreshSession session = new RefreshSession();
        session.setUser(user);
        session.setFamilyId(UUID.randomUUID());
        session.setTokenHash(tokenHash);
        session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshSessionRepository.save(session);

        LoginResponse response = new LoginResponse(
                accessToken,
                jwtTokenService.getExpirationSeconds(),
                userDto
        );

        return new LoginResult(response, rawRefreshToken);
    }

    public record RefreshResult(TokenRefreshResponse response, String newRawRefreshToken) {}

    @Transactional
    public RefreshResult refreshSession(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw AppException.unauthorized("Missing refresh token");
        }

        String tokenHash = jwtTokenService.hashToken(rawRefreshToken);
        Optional<RefreshSession> sessionOpt = refreshSessionRepository.findByTokenHash(tokenHash);

        if (sessionOpt.isEmpty()) {
            throw AppException.unauthorized("Invalid or expired session");
        }

        RefreshSession session = sessionOpt.get();

        // Replay Detection: If consumed token is presented again, revoke entire family!
        if (session.getConsumedAt() != null) {
            log.error("SECURITY ALERT: Refresh token reuse detected for family {}. Revoking entire family.", session.getFamilyId());
            refreshSessionRepository.revokeFamily(session.getFamilyId(), Instant.now());
            throw AppException.unauthorized("Session compromise detected. All sessions revoked.");
        }

        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(Instant.now())) {
            throw AppException.unauthorized("Session revoked or expired");
        }

        User user = session.getUser();
        if (!user.isActive() || user.isDeleted()) {
            throw AppException.unauthorized("User account is inactive or disabled");
        }

        // Consume current session and issue replacement in same family
        Instant now = Instant.now();
        session.setConsumedAt(now);

        String newRawRefreshToken = jwtTokenService.generateOpaqueRefreshToken();
        String newTokenHash = jwtTokenService.hashToken(newRawRefreshToken);

        RefreshSession replacement = new RefreshSession();
        replacement.setUser(user);
        replacement.setFamilyId(session.getFamilyId());
        replacement.setTokenHash(newTokenHash);
        replacement.setExpiresAt(session.getExpiresAt()); // Absolute 7-day family lifetime
        refreshSessionRepository.save(replacement);

        session.setReplacementId(replacement.getId());
        refreshSessionRepository.save(session);

        UserDto userDto = buildEnrichedUserDto(user, null);
        String newAccessToken = jwtTokenService.generateAccessToken(
                user,
                userDto.getActivePlantId(),
                userDto.getActivePlantCode(),
                userDto.getPlantRole(),
                userDto.getAuthorizedPlants() != null ? userDto.getAuthorizedPlants().stream().map(UserPlantMembershipDto::plantCode).toList() : null
        );

        TokenRefreshResponse response = new TokenRefreshResponse(newAccessToken, jwtTokenService.getExpirationSeconds());

        return new RefreshResult(response, newRawRefreshToken);
    }

    @Transactional(readOnly = true)
    public LoginResponse switchPlant(User user, UUID targetPlantId) {
        boolean isGlobalAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName().name());
        List<UserPlantMembership> memberships = membershipRepository.findByUserId(user.getId());

        Plant targetPlant = plantRepository.findByIdAndIsDeletedFalse(targetPlantId)
                .orElseThrow(() -> AppException.notFound("Plant not found with ID: " + targetPlantId));

        if (!isGlobalAdmin) {
            boolean hasAccess = memberships.stream().anyMatch(m -> m.getPlant().getId().equals(targetPlantId));
            if (!hasAccess) {
                throw AppException.forbidden("Cross-tenant access violation: User is not authorized for plant " + targetPlant.getCode());
            }
        }

        UserDto userDto = buildEnrichedUserDto(user, targetPlantId);
        String accessToken = jwtTokenService.generateAccessToken(
                user,
                userDto.getActivePlantId(),
                userDto.getActivePlantCode(),
                userDto.getPlantRole(),
                userDto.getAuthorizedPlants() != null ? userDto.getAuthorizedPlants().stream().map(UserPlantMembershipDto::plantCode).toList() : null
        );

        return new LoginResponse(accessToken, jwtTokenService.getExpirationSeconds(), userDto);
    }

    public UserDto getUserProfile(User user) {
        return buildEnrichedUserDto(user, null);
    }

    public UserDto buildEnrichedUserDto(User user, UUID explicitPlantId) {
        UserDto dto = UserDto.from(user);
        boolean isGlobalAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName().name());
        List<UserPlantMembership> memberships = membershipRepository.findByUserId(user.getId());

        List<UserPlantMembershipDto> authorizedDtos = memberships.stream()
                .filter(m -> !m.getPlant().isDeleted())
                .map(UserPlantMembershipDto::from)
                .toList();
        dto.setAuthorizedPlants(authorizedDtos);

        Plant activePlant = null;
        String activeRole = user.getRole().getName().name();

        if (explicitPlantId != null) {
            activePlant = plantRepository.findByIdAndIsDeletedFalse(explicitPlantId).orElse(null);
            Optional<UserPlantMembership> match = memberships.stream()
                    .filter(m -> m.getPlant().getId().equals(explicitPlantId))
                    .findFirst();
            if (match.isPresent() && match.get().getRole() != null) {
                activeRole = match.get().getRole().getName().name();
            }
        } else {
            Optional<UserPlantMembership> defaultMem = memberships.stream()
                    .filter(UserPlantMembership::isDefault)
                    .findFirst();
            if (defaultMem.isPresent()) {
                activePlant = defaultMem.get().getPlant();
                activeRole = defaultMem.get().getRole().getName().name();
            } else if (!memberships.isEmpty()) {
                activePlant = memberships.get(0).getPlant();
                activeRole = memberships.get(0).getRole().getName().name();
            } else {
                activePlant = plantRepository.findByIdAndIsDeletedFalse(DEFAULT_AUSTIN_PLANT_ID).orElse(null);
            }
        }

        if (activePlant != null) {
            dto.setActivePlantId(activePlant.getId());
            dto.setActivePlantCode(activePlant.getCode());
            dto.setActivePlantName(activePlant.getName());
            dto.setPlantRole(activeRole);
        }

        return dto;
    }

    @Transactional
    public void logout(String rawRefreshToken, User currentUser) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = jwtTokenService.hashToken(rawRefreshToken);
            refreshSessionRepository.findByTokenHash(tokenHash).ifPresent(s -> {
                refreshSessionRepository.revokeFamily(s.getFamilyId(), Instant.now());
            });
        }
        if (currentUser != null) {
            refreshSessionRepository.revokeAllForUser(currentUser, Instant.now());
        }
    }

    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPasswordHash())) {
            throw AppException.badRequest("Current password does not match");
        }

        currentUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        currentUser.setMustChangePassword(false);
        currentUser.setUpdatedAt(Instant.now());
        userRepository.save(currentUser);

        // Revoke all existing refresh sessions on password change
        refreshSessionRepository.revokeAllForUser(currentUser, Instant.now());
    }
}
