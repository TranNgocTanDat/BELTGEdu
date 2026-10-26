package com.example.beprojectweb.config;

import com.example.beprojectweb.enums.Role;
import com.example.beprojectweb.service.CustomOAuth2UserService;
import com.example.beprojectweb.service.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${jwt.signerKey}")
    String signerKey;

    private final CustomOAuth2UserService customOAuth2UserService;

    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity httpSecurity,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        httpSecurity.authorizeHttpRequests(request ->
                request

                        // Permit OPTIONS cho CORS preflight
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Public APIs
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",

                                "/auth/**",
                                "/oauth2/**",

                                "/categories/**",
                                "/products/**"
                        ).permitAll()

                        // User access is additionally scoped by region in UserService.
                        .requestMatchers(HttpMethod.GET, "/users/myInfo")
                        .hasAnyRole(Role.USER.name(), Role.ADMIN.name(), Role.REPORT_WORK.name(), Role.ADMIN_SCHOOL.name())
                        .requestMatchers(HttpMethod.PUT, "/users/me/update")
                        .hasAnyRole(Role.USER.name(), Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/users", "/users/**")
                        .hasAnyRole(Role.USER.name(), Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PATCH, "/users/**")
                        .hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/users/**")
                        .hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/users/**")
                        .hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/users/**")
                        .hasRole(Role.ADMIN.name())

                        .requestMatchers(HttpMethod.GET, "/schools", "/schools/**")
                        .hasAnyRole(Role.USER.name(), Role.ADMIN.name(), Role.ADMIN_SCHOOL.name())
                        .requestMatchers(HttpMethod.POST, "/schools/**")
                        .hasAnyRole(Role.USER.name(), Role.ADMIN.name(), Role.ADMIN_SCHOOL.name())
                        .requestMatchers(HttpMethod.PUT, "/schools/**")
                        .hasAnyRole(Role.USER.name(), Role.ADMIN.name(), Role.ADMIN_SCHOOL.name())
                        .requestMatchers(HttpMethod.DELETE, "/schools/**")
                        .hasAnyRole(Role.USER.name(), Role.ADMIN.name(), Role.ADMIN_SCHOOL.name())

                        .requestMatchers("/registrations/**", "/stem-plans/**", "/digital-plans/**", "/lesson-progress/**")
                        .hasAnyRole(Role.USER.name(), Role.ADMIN.name(), Role.ADMIN_SCHOOL.name())

                        .requestMatchers("/announcements/**", "/school-closures/**")
                        .hasAnyRole(Role.USER.name(), Role.ADMIN.name(), Role.ADMIN_SCHOOL.name())

                        // Users can read their assigned region; only administrators can manage regions.
                        .requestMatchers(HttpMethod.GET, "/regions", "/regions/**")
                        .hasAnyRole(Role.USER.name(), Role.ADMIN.name(), Role.ADMIN_SCHOOL.name())
                        .requestMatchers("/regions/**").hasAnyRole(Role.ADMIN.name(), Role.ADMIN_SCHOOL.name())

                        .requestMatchers("/report-work/**")
                        .hasAnyRole(Role.ADMIN.name(), Role.REPORT_WORK.name())

                        // GAME KEY
                        .requestMatchers(
                                HttpMethod.GET,
                                "/keys/user/**"
                        ).hasRole(Role.USER.name())

                        .requestMatchers(
                                "/keys/**"
                        ).hasAnyRole("USER", "ADMIN")

                        // All remaining requests need auth
                        .anyRequest().authenticated()
        );

        // JWT
        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer ->
                        jwtConfigurer
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                )
        );

        // OAuth2 Login
        httpSecurity.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo ->
                        userInfo.userService(
                                (OAuth2UserService)
                                        customOAuth2UserService
                        )
                )
                .successHandler(oAuth2SuccessHandler)
        );

        // Disable CSRF
        httpSecurity.csrf(csrf -> csrf.disable());

        // Enable CORS
        httpSecurity.cors(Customizer.withDefaults());

        return httpSecurity.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter
                jwtGrantedAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        jwtGrantedAuthoritiesConverter
                .setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter
                jwtAuthenticationConverter =
                new JwtAuthenticationConverter();

        jwtAuthenticationConverter
                .setJwtGrantedAuthoritiesConverter(
                        jwtGrantedAuthoritiesConverter
                );

        return jwtAuthenticationConverter;
    }

    @Bean
    JwtDecoder jwtDecoder() {

        SecretKeySpec secretKeySpec =
                new SecretKeySpec(
                        signerKey.getBytes(),
                        "HS512"
                );

        return NimbusJwtDecoder
                .withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }
}


