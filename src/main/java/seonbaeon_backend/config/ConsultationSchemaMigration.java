package seonbaeon_backend.config;

import java.sql.Connection;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Drops legacy NOT NULL constraints so optional consultation date/time
 * also work on already-provisioned PostgreSQL databases.
 */
@Component
public class ConsultationSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public ConsultationSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product != null && product.toLowerCase().contains("postgresql")) {
                jdbcTemplate.execute("ALTER TABLE consultation_requests ALTER COLUMN preferred_date DROP NOT NULL");
                jdbcTemplate.execute("ALTER TABLE consultation_requests ALTER COLUMN preferred_time DROP NOT NULL");
            }
        }
    }
}
