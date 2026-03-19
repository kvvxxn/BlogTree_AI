package com.navigator.knowledge.global.security.oauth2;

import com.navigator.knowledge.global.security.oauth2.dto.GoogleTokenResponse;
import com.navigator.knowledge.global.security.oauth2.dto.GoogleUserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class GoogleAuthClient {

    // 코파일럿: 직접 생성하지 말고 주입 받으라고 함. @RequiredArgs.. 로 해결
    private final RestTemplateBuilder restTemplateBuilder; // 통신 도구

    @Value("${oauth2.google.client-id}")
    private String clientId;

    @Value("${oauth2.google.client-secret}")
    private String clientSecret;

    @Value("${oauth2.google.redirect-uri}")
    private String redirectUri;

    @Value("${oauth2.google.token-uri}")
    private String tokenUri;

    @Value("${oauth2.google.user-info-uri}")
    private String userInfoUri;

    // 왜 구글 서버 리프레시 토큰은 없는가?
    // 최초 사용자 인증 이후에는 구글 서버와 통신할 일이 없기 때문

    public GoogleTokenResponse getGoogleAccessToken(String code) {
        // 🛡[입구 컷] 인가 코드가 제대로 왔는지 확인
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("인가 코드(code)가 비어있습니다. 정상적인 OAuth2 흐름이 아닙니다.");
        }

        // 빌더를 통해 안전하게 세팅된 restTemplate을 주입받음
        RestTemplate restTemplate = restTemplateBuilder.build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 구글이 요구하는 필수 파라미터들 뭉치기
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        // 구글 토큰소로 POST 요청 발사!
        ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(tokenUri, request, GoogleTokenResponse.class);

        // 리턴용 객체에 주입
        GoogleTokenResponse responseBody = response.getBody();

        // 인가 코드가 인자로 넘어왔다 -> 이미 회원이다
        // 하지만 구글 서버 자체의 오류로 인해 null이 될 수 있음
        if (responseBody == null) {
            throw new RuntimeException("Failed to retrieve access token from Google: Response body is null");
        }

        return responseBody;
    }

    public GoogleUserInfoDto getGoogleUserInfo(GoogleTokenResponse accessTokenResponse) {
        // [입구 컷] 인자가 제대로 왔는지 확인 (Fail-Fast)
        if (accessTokenResponse == null || accessTokenResponse.getAccessToken() == null) {
            throw new IllegalArgumentException("유효하지 않은 구글 액세스 토큰입니다.");
        }

        // 빌더를 통해 안전하게 세팅된 restTemplate을 주입받음
        RestTemplate restTemplate = restTemplateBuilder.build();

        HttpHeaders headers = new HttpHeaders();
        // 헤더에 "Bearer [토큰]" 형식으로 열쇠를 담아야 합니다.
        headers.setBearerAuth(accessTokenResponse.getAccessToken());

        HttpEntity<Void> request = new HttpEntity<>(headers);

        // 구글 정보센터로 GET 요청 발사!
        ResponseEntity<GoogleUserInfoDto> response = restTemplate.exchange(
                userInfoUri,
                HttpMethod.GET,
                request,
                GoogleUserInfoDto.class
        );

        // [출구 확인] 응답 바디가 비어있지 않은지 확인
        GoogleUserInfoDto userInfo = response.getBody();
        if (userInfo == null) {
            throw new RuntimeException("구글 유저 정보를 불러오는데 실패했습니다. (Empty Body)");
        }

        return userInfo;
    }
}
