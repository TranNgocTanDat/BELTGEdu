package com.example.beprojectweb.config;

import com.example.beprojectweb.service.StemEventCatalogService;
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
}
