package seonbaeon_backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Small idempotent schema patch for databases that existed before review
 * reporting was introduced. Safe to run on every application startup.
 */
@Component
public class ReviewSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ReviewSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute(
            "ALTER TABLE reviews ADD COLUMN IF NOT EXISTS reported BOOLEAN NOT NULL DEFAULT FALSE"
        );
        jdbcTemplate.execute(
            "ALTER TABLE reviews ADD COLUMN IF NOT EXISTS report_reason VARCHAR(1000)"
        );
    }
}
