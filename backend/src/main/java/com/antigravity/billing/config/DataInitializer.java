package com.antigravity.billing.config;

import com.antigravity.billing.entity.ExpenseCategory;
import com.antigravity.billing.entity.User;
import com.antigravity.billing.repository.ExpenseCategoryRepository;
import com.antigravity.billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExpenseCategoryRepository expenseCategoryRepository;

    @Value("${app.security.initial-admin.username:${INITIAL_ADMIN_USERNAME:admin}}")
    private String initialAdminUsername;

    @Value("${app.security.initial-admin.email:${INITIAL_ADMIN_EMAIL:owner@mybusiness.local}}")
    private String initialAdminEmail;

    @Value("${app.security.initial-admin.password:${INITIAL_ADMIN_PASSWORD:Admin@SecurePass2026!}}")
    private String initialAdminPassword;

    @Override
    public void run(String... args) {
        // Initialize default administrator ONLY if no admin user exists
        if (!userRepository.existsByUsername(initialAdminUsername)) {
            User initialAdmin = User.builder()
                    .username(initialAdminUsername)
                    .email(initialAdminEmail)
                    .passwordHash(passwordEncoder.encode(initialAdminPassword))
                    .fullName("Business Owner")
                    .role("OWNER")
                    .active(true)
                    .build();
            userRepository.save(initialAdmin);
            log.info("Initial administrator '{}' created successfully.", initialAdminUsername);
        } else {
            log.info("Administrator '{}' already exists. Existing credentials preserved.", initialAdminUsername);
        }

        // Seed default expense categories if none exist
        if (expenseCategoryRepository.count() == 0) {
            List<String[]> defaults = List.of(
                new String[]{"Rent & Utilities", "Office/shop rent, electricity, water bills"},
                new String[]{"Salaries & Wages", "Employee salaries, contract labour payments"},
                new String[]{"Transport & Logistics", "Freight, courier, delivery charges"},
                new String[]{"Marketing & Advertising", "Ads, promotions, printed materials"},
                new String[]{"Office Supplies", "Stationery, printer ink, misc office items"},
                new String[]{"Repairs & Maintenance", "Equipment servicing, building maintenance"},
                new String[]{"Professional Fees", "CA, lawyer, consultant fees"},
                new String[]{"Bank Charges", "Bank fees, payment gateway charges"},
                new String[]{"Miscellaneous", "Other operational expenses"}
            );
            defaults.forEach(d -> expenseCategoryRepository.save(
                ExpenseCategory.builder().name(d[0]).description(d[1]).active(true).build()
            ));
            log.info("Seeded {} default expense categories.", defaults.size());
        }
    }
}