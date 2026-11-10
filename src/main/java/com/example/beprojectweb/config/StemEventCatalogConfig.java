package com.example.beprojectweb.config;

import com.example.beprojectweb.service.StemEventCatalogService;
import com.example.beprojectweb.service.StemEventManagementService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class StemEventCatalogConfig {
    @Bean
    @Order(2)
    ApplicationRunner initializeStemEventCatalog(StemEventCatalogService service) {
        return args -> service.initializeSharedEvents();
    }

    @Bean
    @Order(3)
    ApplicationRunner seedStemEventTemplates(StemEventManagementService service) {
        return args -> service.initializeTemplates();
    }
}
