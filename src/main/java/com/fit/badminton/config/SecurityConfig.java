package com.fit.badminton.config;

import com.fit.badminton.auth.UserAccountService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
    return c.getAuthenticationManager();
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, UserAccountService uds) throws Exception {
    http.userDetailsService(uds)
        .csrf(c -> c
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            .ignoringRequestMatchers("/api/public/**", "/api/auth/login"))
        .authorizeHttpRequests(a -> a
            .requestMatchers("/", "/admin.html", "/index.html", "/api/auth/**", "/session.html", "/css/**", "/js/**",
                "/api/public/**",
                "/api/auth/login", "/actuator/health")
            .permitAll()
            .requestMatchers("/api/owner/**").hasRole("OWNER")
            .requestMatchers("/api/admin/**").hasAnyRole("OWNER", "ADMIN")
            .anyRequest().authenticated())
        .formLogin(f -> f.disable()).httpBasic(b -> b.disable())
        .exceptionHandling(
            e -> e.authenticationEntryPoint((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .accessDeniedHandler((req, res, ex) -> res.sendError(HttpServletResponse.SC_FORBIDDEN)));
    return http.build();
  }
}
