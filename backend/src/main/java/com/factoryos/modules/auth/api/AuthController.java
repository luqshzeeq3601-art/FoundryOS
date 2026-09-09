package com.factoryos.modules.auth.api;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.modules.auth.application.AuthService;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.dto.ChangePasswordRequest;
import com.factoryos.modules.auth.dto.LoginRequest;
import com.factoryos.modules.auth.dto.LoginResponse;
import com.factoryos.modules.auth.dto.TokenRefreshResponse;
import com.factoryos.modules.auth.dto.UserDto;
import com.factoryos.modules.tenant.dto.SwitchPlantRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCsrfToken() {
        String token = UUID.randomUUID().toString();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("csrfToken", token), "CSRF token generated"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthService.LoginResult result = authService.login(request);

        ResponseCookie cookie = ResponseCookie.from("factoryos_refresh", result.rawRefreshToken())
                .httpOnly(true)
                .secure(false) // Set true in production over HTTPS
                .path("/api/v1/auth")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(ApiResponse.ok(result.response(), "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String rawRefreshToken = extractRefreshCookie(request);
        AuthService.RefreshResult result = authService.refreshSession(rawRefreshToken);

        ResponseCookie cookie = ResponseCookie.from("factoryos_refresh", result.newRawRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(ApiResponse.ok(result.response(), "Token refreshed"));
    }

    @PostMapping("/switch-plant")
    public ResponseEntity<ApiResponse<LoginResponse>> switchPlant(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SwitchPlantRequest request
    ) {
        LoginResponse response = authService.switchPlant(currentUser, request.plantId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Plant switched successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @AuthenticationPrincipal User currentUser
    ) {
        String rawRefreshToken = extractRefreshCookie(request);
        authService.logout(rawRefreshToken, currentUser);

        ResponseCookie cookie = ResponseCookie.from("factoryos_refresh", "")
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getUserProfile(currentUser), "Current user details"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse response
    ) {
        authService.changePassword(currentUser, request);

        // Clear refresh cookie
        ResponseCookie cookie = ResponseCookie.from("factoryos_refresh", "")
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(ApiResponse.ok(null, "Password changed successfully"));
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> "factoryos_refresh".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
