package com.antigravity.billing.config;

import com.antigravity.billing.entity.User;
import com.antigravity.billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        userRepository.findByUsername("admin").ifPresent(admin -> {
            if (!passwordEncoder.matches("Admin@SecurePass2026!", admin.getPasswordHash())) {
                admin.setPasswordHash(passwordEncoder.encode("Admin@SecurePass2026!"));
                userRepository.save(admin);
                log.info("Default admin credentials initialized with fresh BCrypt hash.");
            }
        });
    }
}