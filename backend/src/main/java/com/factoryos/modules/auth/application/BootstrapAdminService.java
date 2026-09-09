package com.factoryos.modules.auth.application;

import com.factoryos.modules.auth.domain.Role;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.repository.RoleRepository;
import com.factoryos.modules.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapAdminService {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${factoryos.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${factoryos.bootstrap.admin-email:admin@factoryos.local}")
    private String adminEmail;

    @Value("${factoryos.bootstrap.admin-password:AdminBootstrap2026!Secure}")
    private String adminPassword;

    @Value("${factoryos.bootstrap.admin-name:System Administrator}")
    private String adminName;

    public BootstrapAdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        if (!bootstrapEnabled) {
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmailAndIsDeletedFalse(normalizedEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleType.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleType.ADMIN)));

        User bootstrapAdmin = new User();
        bootstrapAdmin.setEmail(normalizedEmail);
        bootstrapAdmin.setDisplayName(adminName);
        bootstrapAdmin.setPasswordHash(passwordEncoder.encode(adminPassword));
        bootstrapAdmin.setRole(adminRole);
        bootstrapAdmin.setActive(true);
        bootstrapAdmin.setMustChangePassword(false);

        userRepository.save(bootstrapAdmin);
        log.info("FactoryOS initial bootstrap Administrator initialized: {}", normalizedEmail);
    }
}
