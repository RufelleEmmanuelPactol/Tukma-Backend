package org.tukma.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.tukma.auth.services.ModifiedUserServices;



@Configuration
@EnableWebSocket
public class SecurityConfig {


    private final ModifiedUserServices modifiedUserServices;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;


    @Autowired
    public SecurityConfig(ModifiedUserServices modifiedUserServices, JwtAuthenticationFilter authFilter) {
        this.modifiedUserServices = modifiedUserServices;
        jwtAuthenticationFilter = authFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/ws/**").permitAll();
                    auth.requestMatchers("/api/v1/auth/**", "/api/v1/applicant/**", "/debug/**").permitAll();
                    auth.requestMatchers("/api/v1/jobs/get-all-jobs", "/api/v1/jobs/get-job-details/**", "/api/v1/jobs/job-metadata", "/api/v1/jobs/search").permitAll();
                    auth.requestMatchers("/api/v1/resume/cleanup-duplicates").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/survey/questions").permitAll();
                    auth.requestMatchers("/api/v1/survey/answers/**").authenticated();
                    auth.requestMatchers("/api/v1/**").authenticated();
                })
                .headers(headers -> headers
                        .frameOptions().disable()
                        .cacheControl().disable()
                )
                .securityContext(context -> context.requireExplicitSave(false))  // Important for WebSocket
                .logout(logout -> logout.logoutUrl("/api/v1/auth/logout"))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(modifiedUserServices)
                .passwordEncoder(passwordEncoder()).and().build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

}
