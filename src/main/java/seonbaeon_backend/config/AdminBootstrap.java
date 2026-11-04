package seonbaeon_backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import seonbaeon_backend.entity.User;
import seonbaeon_backend.repository.UserRepository;

@Configuration
public class AdminBootstrap {
    @Bean
    CommandLineRunner createAdminIfConfigured(UserRepository users) {
        return args -> {
            String email = System.getenv("ADMIN_EMAIL");
            String password = System.getenv("ADMIN_PASSWORD");
            if (email == null || email.isBlank() || password == null || password.length() < 12) return;
            String normalized = email.trim().toLowerCase();
            User admin = users.findByEmailIgnoreCase(normalized).orElseGet(User::new);
            if (admin.getId() == null) {
                admin.setName("Pick My Teacher Admin");
                admin.setEmail(normalized);
            }
            admin.setRole("ADMIN");
            admin.setPassword(new BCryptPasswordEncoder().encode(password));
            users.save(admin);
        };
    }
}
