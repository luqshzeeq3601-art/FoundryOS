package com.factoryos.modules.auth.infrastructure;

import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserRepository userRepository) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        response.setHeader("X-Request-ID", requestId);
        MDC.put("reqId", requestId);
        MDC.put("traceId", requestId);

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtTokenService.validateAndExtractClaims(token);
                String userIdStr = claims.getSubject();
                UUID userId = UUID.fromString(userIdStr);

                // Always verify active status directly from DB
                Optional<User> userOpt = userRepository.findByIdAndIsDeletedFalse(userId);
                if (userOpt.isPresent() && userOpt.get().isActive()) {
                    User user = userOpt.get();
                    MDC.put("userId", user.getId().toString());

                    // Restrict temporary-password users to password change endpoints only
                    boolean isTemporary = user.isMustChangePassword();
                    String path = request.getRequestURI();
                    if (isTemporary && !path.startsWith("/api/v1/auth/change-password") && !path.startsWith("/api/v1/auth/me") && !path.startsWith("/api/v1/auth/logout")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Temporary password must be changed before accessing operational tools.");
                        return;
                    }

                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().name());
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            Collections.singletonList(authority)
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                // Invalid token - clear context
                SecurityContextHolder.clearContext();
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
