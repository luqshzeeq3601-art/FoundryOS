package com.factoryos.modules.auth.infrastructure;

import com.factoryos.modules.tenant.context.TenantContextFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TenantContextFilter tenantContextFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            TenantContextFilter tenantContextFilter,
            RateLimitingFilter rateLimitingFilter,
            @Value("${factoryos.cors.allowed-origins:http://localhost:5173,http://localhost:80,http://localhost}") String allowedOriginsStr
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.tenantContextFilter = tenantContextFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.allowedOrigins = Arrays.asList(allowedOriginsStr.split(","));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(antMatcher("/api/v1/auth/login"), antMatcher("/api/v1/auth/refresh"), antMatcher("/api/v1/auth/csrf")).permitAll()
                        .requestMatchers(antMatcher("/actuator/health/**"), antMatcher("/actuator/info")).permitAll()
                        .requestMatchers(antMatcher("/api/v1/users"), antMatcher("/api/v1/users/**")).hasRole("ADMIN")
                        .requestMatchers(antMatcher("/api/v1/audit-events"), antMatcher("/api/v1/audit-events/**")).hasAnyRole("ADMIN", "PRODUCTION_MANAGER", "ENGINEER")
                        .requestMatchers(antMatcher(HttpMethod.DELETE, "/api/v1/machines"), antMatcher(HttpMethod.DELETE, "/api/v1/machines/**")).hasRole("ADMIN")
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/v1/machines"), antMatcher(HttpMethod.POST, "/api/v1/machines/**")).hasAnyRole("ADMIN", "PRODUCTION_MANAGER", "ENGINEER")
                        .requestMatchers(antMatcher(HttpMethod.PUT, "/api/v1/machines"), antMatcher(HttpMethod.PUT, "/api/v1/machines/**")).hasAnyRole("ADMIN", "PRODUCTION_MANAGER", "ENGINEER")
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/v1/production-orders"), antMatcher(HttpMethod.POST, "/api/v1/production-orders/**")).hasAnyRole("ADMIN", "PRODUCTION_MANAGER")
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/v1/maintenance-work-orders"), antMatcher(HttpMethod.POST, "/api/v1/maintenance-work-orders/**")).hasAnyRole("ADMIN", "PRODUCTION_MANAGER", "ENGINEER", "TECHNICIAN")
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/v1/downtime"), antMatcher(HttpMethod.POST, "/api/v1/downtime/**")).hasAnyRole("ADMIN", "PRODUCTION_MANAGER", "ENGINEER", "TECHNICIAN", "OPERATOR")
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/v2/telemetry/ingest"), antMatcher("/api/v2/telemetry/ingest/**")).hasAnyRole("ADMIN", "ENGINEER")
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/v2/telemetry/machines/*/tags"), antMatcher("/api/v2/telemetry/machines/*/tags/**")).hasAnyRole("ADMIN", "ENGINEER")
                        .requestMatchers(antMatcher(HttpMethod.DELETE, "/api/v2/telemetry/machines/*/tags/*")).hasRole("ADMIN")
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/v2/telemetry/retention/**")).hasRole("ADMIN")
                        .requestMatchers(antMatcher("/api/v2/telemetry/**")).authenticated()
                        .requestMatchers(antMatcher("/api/v2/downtime/**")).authenticated()
                        .requestMatchers(antMatcher("/api/v2/barcode/**")).authenticated()
                        .requestMatchers(antMatcher("/api/v2/hierarchy/**")).authenticated()
                        .requestMatchers(antMatcher("/api/v2/analytics/**")).authenticated()
                        .requestMatchers(antMatcher("/api/v2/edge/**")).authenticated()
                        .requestMatchers(antMatcher("/api/v2/sop/**")).authenticated()
                        .requestMatchers(antMatcher("/api/v2/vibration/**")).authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantContextFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Enforce BCrypt cost 12 as required by Security.md & CODEX.md
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-ID", "X-CSRF-Token", "X-Plant-ID", "X-Plant-Code"));
        configuration.setExposedHeaders(List.of("X-Request-ID", "Retry-After", "X-Plant-ID", "X-Plant-Code"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
