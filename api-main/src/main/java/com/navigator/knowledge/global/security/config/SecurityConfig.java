package com.navigator.knowledge.global.security.config;

import com.navigator.knowledge.global.security.jwt.JwtAuthenticationEntryPoint;
import com.navigator.knowledge.global.security.jwt.JwtAuthenticationFilter;
import com.navigator.knowledge.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor // 요원들을 주입(DI)받기 위해 추가
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CORS 설정 (프론트엔드 연동을 위한 대문 개방)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. CSRF, FormLogin, HttpBasic 끄기 (REST API 기본 세팅 유지)
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // 3. 세션 정책은 STATELESS 유지 (JWT니까)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. H2 콘솔 프레임 거부 해결
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                )

                // 5. 거절 처리반 등록 (만료/위조 토큰 들고오면 401 에러)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // 6. 구역별 출입 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 기존 프리패스 구역 유지
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/login/oauth2/code/**").permitAll()

                        // 임시 개방 종료 나머지는 모두 신분증(토큰) 필수
                        .anyRequest().authenticated()
                )

                // 7. 우리의 문지기(JwtAuthenticationFilter)를 출입구 맨 앞에 배치
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 세부 설정 (프론트엔드 localhost:3000과 통신하기 위한 필수 설정)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(allowedOrigins); // 프론트엔드 주소: 3000이 아닐 수 있음
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization")); // 프론트가 응답 헤더에서 토큰을 읽을 수 있게 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
