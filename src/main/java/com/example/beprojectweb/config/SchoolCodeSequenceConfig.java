package com.example.beprojectweb.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SchoolCodeSequenceConfig {
    @Bean
    ApplicationRunner initializeSchoolCodeSequence(JdbcTemplate jdbc) {
        return args -> {
            jdbc.execute("CREATE TABLE IF NOT EXISTS school_code_sequence (sequence_name varchar(40) NOT NULL PRIMARY KEY, current_value bigint NOT NULL)");
            jdbc.update("INSERT IGNORE INTO school_code_sequence (sequence_name, current_value) " +
                    "SELECT 'school', COALESCE(MAX(CAST(SUBSTRING(code, 4) AS UNSIGNED)), 0) " +
                    "FROM schools WHERE code REGEXP '^TR-[0-9]+$'");
        };
    }
}
