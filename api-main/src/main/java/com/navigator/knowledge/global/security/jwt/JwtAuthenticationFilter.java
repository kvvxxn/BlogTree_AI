package com.navigator.knowledge.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. 프론트엔드가 보낸 편지 봉투(헤더)에서 출입증(토큰)만 쏙 꺼냅니다.
        String token = resolveToken(request);

        try {
            // 2. 출입증이 있고, 감식반(JwtProvider)이 진짜라고 확인해주면
            if (token != null && jwtProvider.validateToken(token)) {

                // 3. 토큰 안에서 이메일과 권한(Role)을 빼옵니다. (DB 조회 안 함 완벽한 무상태성)
                String email = jwtProvider.getEmailFromToken(token);
                String role = jwtProvider.getRoleFromToken(token);

                // 4. 스프링 시큐리티 형님이 읽을 수 있도록 권한을 포장합니다.
                List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

                // 5. 스프링 시큐리티 전용 임시 플라스틱 사원증(Authentication)을 발급합니다.
                // (이메일을 principal로, 비밀번호는 없으니 "", 권한을 넣습니다)
                Authentication authentication = new UsernamePasswordAuthenticationToken(email, "", authorities);

                // 6. 우리 건물의 VIP 명부(SecurityContextHolder)에 이 사원증을 걸어둡니다.
                // 이제 이 요청이 컨트롤러에 도착할 때까지 이 사람은 "인증된 유저"로 대우받습니다.
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("인증 성공! 이메일: {}, 권한: {}", email, role);
            }
        } catch (IllegalArgumentException e) {
            // JwtProvider가 에러를 뱉었을 때 (만료됨, 위조됨 등)
            // 필터 밖으로 에러가 터지지 않게 일단 잡고, 다음 부서(EntryPoint)가 알 수 있게 메모만 남겨둡니다.
            request.setAttribute("exception", e.getMessage());
            log.warn("토큰 검증 실패: {}", e.getMessage());
        }

        // 7. 내 할 일(검문)은 끝났으니, 다음 필터나 목적지(컨트롤러)로 요청을 넘겨줍니다.
        filterChain.doFilter(request, response);
    }

    // 헤더에서 "Bearer 어쩌구저쩌구" 하는 부분 중 "어쩌구저쩌구(토큰)"만 잘라내는 헬퍼 메서드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 글자(7칸) 빼고 진짜 토큰만 반환
        }
        return null;
    }
}
