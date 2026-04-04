package com.jivRas.groceries.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration.
 *
 * NOTE: CORS is handled exclusively by CorsConfig.java which reads
 * frontend.url from application.properties. There is NO CorsConfigurationSource
 * bean here — defining one here would override CorsConfig and cause CORS failures.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(UserDetailsService userDetailsService, JwtAuthFilter jwtAuthFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())  // delegates to CorsConfig.java WebMvcConfigurer
            .authorizeHttpRequests(auth -> auth
                // Preflight requests — always permitted
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ── Public auth endpoints ───────────────────────────────────
                .requestMatchers("/api/users/login",
                                 "/api/users/register",
                                 "/api/users/refresh",
                                 "/api/users/logout").permitAll()

                // H2 console (dev only)
                .requestMatchers("/h2-console/**").permitAll()

                // ── Truly public read-only resources ────────────────────────
                .requestMatchers("/images/**").permitAll()
                .requestMatchers("/api/locations/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()

                // Cart: guests allowed via X-Guest-Id header
                .requestMatchers("/api/cart/**").permitAll()

                // ── Role-permissions CRUD ────────────────────────────────────
                .requestMatchers("/api/permissions/**").authenticated()

                // ── All other endpoints: must have a valid JWT ───────────────
                // Fine-grained role enforcement is done by DynamicAuthorizationService
                // inside each controller method.
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ── NO corsConfigurationSource() bean here ──────────────────────────────
    // CorsConfig.java (WebMvcConfigurer) handles CORS globally.
    // Having two CORS configurations causes Spring Security to use this one
    // for the filter chain, silently ignoring CorsConfig — which broke CORS.
}