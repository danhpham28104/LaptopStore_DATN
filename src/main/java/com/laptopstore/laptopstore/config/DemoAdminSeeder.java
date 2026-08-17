package com.laptopstore.laptopstore.config;

import com.laptopstore.laptopstore.Repository.RoleRepository;
import com.laptopstore.laptopstore.Repository.UserRepository;
import com.laptopstore.laptopstore.entity.Role;
import com.laptopstore.laptopstore.entity.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DemoAdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoAdminSeeder.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${demo.admin.enabled:false}")
    private boolean demoAdminEnabled;

    @Value("${demo.admin.email:}")
    private String demoAdminEmail;

    @Value("${demo.admin.password:}")
    private String demoAdminPassword;

    @Bean
    CommandLineRunner seedDemoAdmin() {
        return args -> {
            if (!demoAdminEnabled || demoAdminEmail == null || demoAdminEmail.isBlank()) {
                return;
            }

            if (userRepository.findByEmail(demoAdminEmail).isPresent()) {
                log.info("[DEMO ADMIN] Account with email '{}' already exists. Skipping password reset.", demoAdminEmail);
                return;
            }

            // Ensure ROLE_ADMIN role exists in database
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("ROLE_ADMIN");
                        r.setDescription("Administrator");
                        return roleRepository.save(r);
                    });

            String usernamePrefix = demoAdminEmail.contains("@") 
                    ? demoAdminEmail.substring(0, demoAdminEmail.indexOf("@")) 
                    : demoAdminEmail;

            String finalUsername = usernamePrefix;
            int counter = 1;
            while (userRepository.findByUsername(finalUsername).isPresent()) {
                finalUsername = usernamePrefix + "_" + counter++;
            }

            User demoAdmin = new User();
            demoAdmin.setUsername(finalUsername);
            demoAdmin.setEmail(demoAdminEmail);

            String passToSet = (demoAdminPassword != null && !demoAdminPassword.isBlank()) 
                    ? demoAdminPassword 
                    : "admin123";

            demoAdmin.setPassword(passwordEncoder.encode(passToSet));
            demoAdmin.setFullName("Demo Administrator");
            demoAdmin.setPhone("0900000000");
            demoAdmin.setDeleted(false);
            demoAdmin.setRoles(Set.of(adminRole));

            userRepository.save(demoAdmin);
            log.info("[DEMO ADMIN] Demo Admin created successfully. Email: {}, Username: {}", demoAdminEmail, finalUsername);
        };
    }
}
