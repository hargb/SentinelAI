
package com.sentinelai;

import java.util.List;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:3001",
                 "http://localhost:3002"
        ));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH",
                "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
@Bean
public UserDetailsService userDetailsService() {
    UserDetails reviewer = User.withUsername("sentinel-dev")
            .password("{noop}" + System.getenv("SENTINEL_DEV_PASSWORD"))
            .roles("REVIEWER")
            .build();

    UserDetails normalUser = User.withUsername("sentinel-reader")
            .password("{noop}" + System.getenv("SENTINEL_READER_PASSWORD"))
            .roles("USER")
            .build();

    return new InMemoryUserDetailsManager(reviewer, normalUser);
}
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                    "/api/v1/policies/evaluate",
                    "/api/v1/approvals/**"
            ))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health").permitAll()
                     .requestMatchers("/api/v1/audit-logs/**").hasRole("REVIEWER")
                    .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
