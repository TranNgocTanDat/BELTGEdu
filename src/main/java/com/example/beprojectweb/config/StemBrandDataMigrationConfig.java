package com.example.beprojectweb.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class StemBrandDataMigrationConfig {
    @Bean
    @Order(10)
    ApplicationRunner migrateStemBrandData(JdbcTemplate jdbc) {
        return args -> {
            String migrationKey = "stem-brand-assets-v1";
            jdbc.execute("CREATE TABLE IF NOT EXISTS curriculum_seed_history (seed_key varchar(80) NOT NULL PRIMARY KEY, seeded_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            Integer completed = jdbc.queryForObject("SELECT COUNT(*) FROM curriculum_seed_history WHERE seed_key = ?", Integer.class, migrationKey);
            if (completed != null && completed > 0) return;

            // Preserve the original URL and copy it to the AIG variant only when that variant is empty.
            jdbc.update("""
                    UPDATE stem_lessons
                    SET drive_image_url_aig = drive_image_url
                    WHERE drive_image_url IS NOT NULL
                      AND TRIM(drive_image_url) <> ''
                      AND (drive_image_url_aig IS NULL OR TRIM(drive_image_url_aig) = '')
                    """);

            // Legacy ETECH registrations retain the current AIG artwork until someone selects ETECH explicitly.
            jdbc.update("""
                    UPDATE registrations
                    SET stem_brand_identity = 'AIG'
                    WHERE stem_program = 'ETECH'
                      AND (stem_brand_identity IS NULL OR TRIM(stem_brand_identity) = '')
                    """);
            jdbc.update("INSERT INTO curriculum_seed_history (seed_key) VALUES (?)", migrationKey);
        };
    }
}
