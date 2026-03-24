package com.navigator.knowledge.global.security.oauth2;

import com.navigator.knowledge.global.security.oauth2.dto.GoogleTokenResponse;
import com.navigator.knowledge.global.security.oauth2.dto.GoogleUserInfoDto;
import com.navigator.knowledge.global.security.oauth2.properties.GoogleOAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleAuthClient {

    private final RestTemplate restTemplate;
    private final GoogleOAuthProperties googleOAuthProperties;

    // 왜 구글 서버 리프레시 토큰은 없는가?
    // 최초 사용자 인증 이후에는 구글 서버와 통신할 일이 없기 때문
    public GoogleTokenResponse getGoogleAccessToken(String code) {
        // 인가 코드가 제대로 왔는지 확인
        if (code == null || code.isBlank()) {
            log.warn("Google access token code is null or empty");
            throw new IllegalArgumentException("인가 코드(code)가 비어있습니다. 정상적인 OAuth2 흐름이 아닙니다.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 구글이 요구하는 필수 파라미터들 뭉치기
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleOAuthProperties.clientId());
        params.add("client_secret", googleOAuthProperties.clientSecret());
        params.add("redirect_uri", googleOAuthProperties.redirectUri());
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        log.info("[Google OAuth] Requesting Access Token... (tokenUri: {})", googleOAuthProperties.tokenUri());

        ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(googleOAuthProperties.tokenUri(), request, GoogleTokenResponse.class);

        GoogleTokenResponse responseBody = response.getBody();

        // 인가 코드가 인자로 넘어왔다 -> 이미 회원이다
        // 하지만 구글 서버 자체의 오류로 인해 null이 될 수 있음
        if (responseBody == null) {
            log.error("[Google OAuth] Google Access Token response is null");
            throw new RuntimeException("Failed to retrieve access token from Google: Response body is null");
        }

        log.info("[Google OAuth] Google Access Token request successful");
        return responseBody;
    }

    public GoogleUserInfoDto getGoogleUserInfo(GoogleTokenResponse accessTokenResponse) {
        if (accessTokenResponse == null || accessTokenResponse.getAccessToken() == null) {
            log.warn("Google access token response is null or empty");
            throw new IllegalArgumentException("유효하지 않은 구글 액세스 토큰입니다.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessTokenResponse.getAccessToken());

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<GoogleUserInfoDto> response = restTemplate.exchange(
                googleOAuthProperties.userInfoUri(),
                HttpMethod.GET,
                request,
                GoogleUserInfoDto.class
        );

        GoogleUserInfoDto userInfo = response.getBody();
        if (userInfo == null) {
            log.error("[Google OAuth] Google User Info response is null");
            throw new RuntimeException("구글 유저 정보를 불러오는데 실패했습니다. (Empty Body)");
        }

        log.info("[Google OAuth] Google UserInfo request successful");
        return userInfo;
    }
}
