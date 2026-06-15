package com.example.beprojectweb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")

                // Frontend Vite
                .allowedOrigins("http://localhost:5173", "https://ltgedu.id.vn",
                        "https://www.ltgedu.id.vn", "https://contact.ltg.edu.vn", "https://www.contact.ltg.edu.vn" )


                // Cho phép tất cả method cần thiết
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )

                // Cho phép tất cả headers
                .allowedHeaders("*")

                // Cho phép gửi token/cookie
                .allowCredentials(true);
    }
}