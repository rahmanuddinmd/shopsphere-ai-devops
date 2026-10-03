package com.shopsphere.config;

import com.shopsphere.entity.Role;
import com.shopsphere.entity.RoleName;
import com.shopsphere.entity.User;
import com.shopsphere.repository.RoleRepository;
import com.shopsphere.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first ADMIN account from ADMIN_EMAIL / ADMIN_PASSWORD at startup if it does not exist.
 * No admin password is ever stored in source code or in a SQL migration.
 */
@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final AppProperties props;
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public AdminUserInitializer(AppProperties props, UserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = props.admin().email();
        String password = props.admin().password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("ADMIN_EMAIL / ADMIN_PASSWORD not set - no admin account bootstrapped");
            return;
        }
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        if (password.length() < 8) {
            log.warn("ADMIN_PASSWORD shorter than 8 characters - admin account not created");
            return;
        }
        User admin = new User(email.toLowerCase(), encoder.encode(password), "ShopSphere Admin");
        admin.getRoles().add(role(RoleName.ADMIN));
        admin.getRoles().add(role(RoleName.CUSTOMER));
        users.save(admin);
        log.info("Bootstrap admin account created for {}", email);
    }

    private Role role(RoleName name) {
        return roles.findByName(name).orElseThrow(() -> new IllegalStateException("Missing role " + name));
    }
}
