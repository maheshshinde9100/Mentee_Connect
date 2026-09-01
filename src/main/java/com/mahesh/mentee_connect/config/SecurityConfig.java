package com.mahesh.mentee_connect.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import com.mahesh.mentee_connect.security.jwt.JwtAuthenticationFilter;
import com.mahesh.mentee_connect.security.jwt.AuthEntryPointJwt;
import com.mahesh.mentee_connect.security.services.UserDetailsServiceImpl;
import com.mahesh.mentee_connect.security.jwt.JwtUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Comma-separated origin patterns for browser clients.
     * Allowed to '*' to ensure Android WebViews and custom schemes are supported.
     */
    @Value("${app.cors.allowed-origin-patterns:*}")
    private String corsAllowedOriginPatterns;

    @Bean
    public JwtAuthenticationFilter authenticationJwtTokenFilter() {
        return new JwtAuthenticationFilter(jwtUtils, userDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'self'"))
            )
            .authorizeHttpRequests(auth ->
                auth.requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/api-docs/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/notifications/auth-test").permitAll()
                    .requestMatchers("/notifications/auth-status").permitAll()
                    // Admin APIs — method-level @PreAuthorize also applies
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    // WebSocket signaling (JWT validated in handshake handler where configured)
                    .requestMatchers("/signal/**", "/ws/**").permitAll()
                    // All other REST endpoints require a valid JWT (web + Android)
                    .anyRequest().authenticated()
            );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse allowed origin patterns from config
        List<String> patterns = Arrays.stream(corsAllowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        // If the pattern is literally "*" we must NOT use allowCredentials=true
        // (CORS spec forbids wildcard + credentials). Instead, list origins explicitly.
        boolean isWildcard = patterns.size() == 1 && patterns.get(0).equals("*");
        if (isWildcard) {
            // Explicit list of allowed origins - covers web frontend + Android dev builds
            configuration.setAllowedOriginPatterns(Arrays.asList(
                "https://menteeconnect.vercel.app",
                "https://*.vercel.app",        // Vercel preview deployments
                "http://localhost:3000",
                "http://localhost:8080",
                "http://127.0.0.1:3000",
                "http://10.0.2.2:8080",        // Android emulator -> host machine
                "http://10.0.2.2:3000",
                "capacitor://*",               // Capacitor/Ionic Android/iOS WebView
                "ionic://*",
                "https://menteeconnect-backend.onrender.com"
            ));
        } else {
            configuration.setAllowedOriginPatterns(patterns);
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "Accept",
            "X-Requested-With", "Origin", "Content-Disposition", "Cache-Control"
        ));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
