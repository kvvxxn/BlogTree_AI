package com.navigator.knowledge.global.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 끄기
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // 1. 세션 정책은 STATELESS 유지 (JWT니까요!)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. ⭐ H2 콘솔 프레임 거부 해결 (이게 없으면 연결 거부 뜹니다)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                )

                .authorizeHttpRequests(auth -> auth
                        // 3. ⭐ H2 콘솔 주소는 시큐리티 검사 예외로 확실히 등록
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/login/oauth2/code/**").permitAll()
                        .requestMatchers("/api/auth/test/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
