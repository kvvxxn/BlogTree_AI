package com.navigator.knowledge.domain.auth.service;

import com.navigator.knowledge.domain.auth.dto.AuthDto;
import com.navigator.knowledge.domain.auth.repository.InMemoryRefreshTokenRepository;
import com.navigator.knowledge.domain.user.entity.Role;
import com.navigator.knowledge.domain.user.entity.User;
import com.navigator.knowledge.domain.auth.repository.RefreshTokenRepository;
import com.navigator.knowledge.domain.user.repository.UserRepository;
import com.navigator.knowledge.global.security.jwt.JwtProvider;
import com.navigator.knowledge.global.security.oauth2.GoogleAuthClient;
import com.navigator.knowledge.global.security.oauth2.dto.GoogleTokenResponse;
import com.navigator.knowledge.global.security.oauth2.dto.GoogleUserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2Service {
    private final GoogleAuthClient googleAuthClient;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    // 인가 코드를 통해 구글 로그인 처리
    public AuthDto.LoginResponse googleLogin(String code) {
        // GoogleAuthClient에게 일 시키기
        // 1. 구글 access token 받아오기
        GoogleTokenResponse googleToken = googleAuthClient.getGoogleAccessToken(code);

        // 2. access token으로 유저 정보 (JSON) 받아오기
        GoogleUserInfoDto userInfoDto = googleAuthClient.getGoogleUserInfo(googleToken);

        // 3. 유저 정보 조회 및 DB 저장 (신규 가입 또는 기존 정보 갱신)
        String email = userInfoDto.getEmail();

        // 유저를 저장한 다음 우리 서비스만의 JWT를 만들려면 유저의 정보가 필요
        // 뽑아올 객체가 필요해서 User user에 db 조회 결과를 저장
        User user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    // 이미 기존 유저가 존재하면 이름과 프로필을 최신화
                    existingUser.update(userInfoDto.getName(), userInfoDto.getPicture());
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    // 신규라면 새 엔티티로 저장
                    User newUser = User.builder()
                            .email(email)
                            .name(userInfoDto.getName())
                            .profileImageUrl(userInfoDto.getPicture())
                            .role(Role.USER)
                            .build();
                    return userRepository.save(newUser);
                });

        // 4. 우리 서비스 전용 JWT (Access / Refresh) 토큰 생성
        String accessToken = jwtProvider.createAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getEmail());

        // 5. Refresh Token을 메모리에 저장 (ConcurrentHashMap)
        // 추후 운영, 배포 시 Redis 등으로 변경 예정
        // Map 구조이므로 이미 이메일(key)이 존재하면 알아서 새 토큰으로 덮어씌움
        refreshTokenRepository.save(user.getEmail(), refreshToken);

        // 6. 프론트엔드에게 줄 택배 상자에 포장해서 반환
        String message = "구글 로그인 및 토큰 발급 성공!";
        return new AuthDto.LoginResponse(message, accessToken, refreshToken);
    }

    // HashMap 테스트용 메서드
    //public Map<String, String> getAllTokensForTest() {
    //    if (refreshTokenRepository instanceof InMemoryRefreshTokenRepository inMemoryRepo) {
    //        return inMemoryRepo.getTokenMap();
    //    }
    //    return Collections.emptyMap();
    //}
}
