package seonbaeon_backend;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class LegacyProgressCleanup {
    @Bean
    ApplicationRunner removeDemoProgressNotes(JdbcTemplate jdbc) {
        return args -> cleanup(jdbc);
    }

    @Transactional
    void cleanup(JdbcTemplate jdbc) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS app_data_migrations (migration_key VARCHAR(120) PRIMARY KEY, applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        Integer applied = jdbc.queryForObject(
            "SELECT COUNT(*) FROM app_data_migrations WHERE migration_key = ?",
            Integer.class,
            "2026-11-remove-demo-progress-notes"
        );
        if (applied != null && applied > 0) return;

        jdbc.update("DELETE FROM student_progress_notes");
        jdbc.update(
            "INSERT INTO app_data_migrations (migration_key) VALUES (?)",
            "2026-11-remove-demo-progress-notes"
        );
    }
}
