package com.example.beprojectweb.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class StemCatalogSeedConfig {
    @Bean
    @Order(1)
    ApplicationRunner seedStemCatalog(DataSource dataSource, JdbcTemplate jdbc) {
        return args -> {
            jdbc.execute("CREATE TABLE IF NOT EXISTS curriculum_seed_history (seed_key varchar(80) NOT NULL PRIMARY KEY, seeded_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            Integer done = jdbc.queryForObject("SELECT COUNT(*) FROM curriculum_seed_history WHERE seed_key = ?", Integer.class, "stem-catalog-v1");
            if (done != null && done == 0) {
                Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM stem_courses", Integer.class);
                if (existing == null || existing == 0) {
                    new ResourceDatabasePopulator(new ClassPathResource("stem-catalog-seed.sql")).execute(dataSource);
                }
                jdbc.update("INSERT INTO curriculum_seed_history (seed_key) VALUES (?)", "stem-catalog-v1");
            }
        };
    }
}
