package com.devsop.project.apartmentinvoice.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.devsop.project.apartmentinvoice.entity.UserAccount;
import com.devsop.project.apartmentinvoice.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserAccountRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create admin user if not exists
        if (userRepo.findByUsername("admin").isEmpty()) {
            UserAccount admin = new UserAccount();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("1234"));
            admin.setRole("ADMIN");
            userRepo.save(admin);
            log.info("Created default admin user: admin/1234");
        }

        // Create staff user if not exists
        if (userRepo.findByUsername("staff").isEmpty()) {
            UserAccount staff = new UserAccount();
            staff.setUsername("staff");
            staff.setPassword(passwordEncoder.encode("staff123"));
            staff.setRole("STAFF");
            userRepo.save(staff);
            log.info("Created default staff user: staff/staff123");
        }

        log.info("Data initialization complete");
    }
}
