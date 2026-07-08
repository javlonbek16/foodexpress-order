package com.foodexpress.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Running database migrations/fixes...");
            jdbcTemplate.execute("ALTER TABLE courier_order_history ALTER COLUMN order_completed_at DROP NOT NULL");
            log.info("Successfully dropped NOT NULL constraint on courier_order_history.order_completed_at");
        } catch (Exception e) {
            log.warn("Could not alter table (it might not exist or constraint already dropped): {}", e.getMessage());
        }
    }
}
