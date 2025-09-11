package com.goldresto.config;

import com.goldresto.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/login", "/error").permitAll()
                .requestMatchers("/api/printer/**").permitAll()
                .requestMatchers("/pos/panier/*/validate").permitAll()
                
                // POS and Sales
                .requestMatchers("/pos/panier/*/validate").permitAll()
                .requestMatchers("/pos/**").hasAnyAuthority("ROLE_EMPLOYEE", "ROLE_ADMIN", "ROLE_OWNER")
                .requestMatchers("/panier/**").hasAnyAuthority("ROLE_EMPLOYEE", "ROLE_ADMIN", "ROLE_OWNER")
                
                // Product and Stock Management (Admin access)
                .requestMatchers("/produits/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_OWNER")
                .requestMatchers("/stock/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_OWNER")
                
                // Reports and System Management (Owner only)
                .requestMatchers("/reports/**").hasAuthority("ROLE_OWNER")
                .requestMatchers("/users/**").hasAuthority("ROLE_OWNER")
                .requestMatchers("/system/**").hasAuthority("ROLE_OWNER")
                
                // All other URLs require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            );
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
