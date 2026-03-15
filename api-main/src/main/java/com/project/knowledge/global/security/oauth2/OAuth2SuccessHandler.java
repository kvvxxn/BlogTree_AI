package com.project.knowledge.global.security.oauth2;

import com.project.knowledge.domain.user.entity.RefreshToken;
import com.project.knowledge.domain.user.repository.RefreshTokenRepository;
import com.project.knowledge.global.security.jwt.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository; // DB 저장소 추가!

    // 롬복 없이 생성자 주입! (방금 만든 토큰 공장을 가져옵니다)
    public OAuth2SuccessHandler(JwtProvider jwtProvider, RefreshTokenRepository refreshTokenRepository) {
        this.jwtProvider = jwtProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 1. 방금 구글 로그인에 성공한 유저 정보 꺼내기 (우리가 만든 포장지로 형변환)
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getName(); // 포장지에서 이메일 꺼내기
        String role = oAuth2User.getAuthorities().iterator().next().getAuthority(); // 권한(ROLE_USER) 꺼내기

        // 2. 토큰 공장 돌려서 Access Token (사원증) 발급!
        String accessToken = jwtProvider.createAccessToken(email, role);
        String refreshToken = jwtProvider.createRefreshToken(email);

        // 3. [핵심] Refresh Token을 DB에 저장 (없으면 생성, 있으면 업데이트)
        RefreshToken tokenEntity = refreshTokenRepository.findByEmail(email)
                .map(entity -> {
                    entity.updateToken(refreshToken);
                    return entity;
                })
                .orElse(new RefreshToken(email, refreshToken));

        refreshTokenRepository.save(tokenEntity);

        // 4. 응답 형식을 JSON으로 세팅 (나 이제부터 화면 말고 데이터 줄게!)
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        // 5. 예쁜 JSON 문자열 빚어내기
        // 실제로는 Gson이나 Jackson 라이브러리를 쓰지만, 직관적으로 이렇게 짜도 됩니다.
        String jsonResponse = "{"
                + "\"message\": \"구글 로그인 성공!\","
                + "\"accessToken\": \"" + accessToken + "\","
                + "\"refreshToken\": \"" + refreshToken + "\""
                + "}";

        // 6. 바디(Body)에 꾹꾹 눌러 담아서 프론트엔드로 쏴주기
        response.getWriter().write(jsonResponse);
    }
}
