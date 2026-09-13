package com.antigravity.billing.config;

import com.antigravity.billing.entity.ExpenseCategory;
import com.antigravity.billing.entity.User;
import com.antigravity.billing.repository.ExpenseCategoryRepository;
import com.antigravity.billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void run(String... args) {
        // Re-hash admin password if needed
        userRepository.findByUsername("admin").ifPresent(admin -> {
            if (!passwordEncoder.matches("Admin@SecurePass2026!", admin.getPasswordHash())) {
                admin.setPasswordHash(passwordEncoder.encode("Admin@SecurePass2026!"));
                userRepository.save(admin);
                log.info("Default admin credentials initialized with fresh BCrypt hash.");
            }
        });

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