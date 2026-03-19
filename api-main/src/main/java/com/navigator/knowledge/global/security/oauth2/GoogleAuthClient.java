package com.navigator.knowledge.global.security.oauth2;

import com.navigator.knowledge.global.security.oauth2.dto.GoogleTokenResponse;
import com.navigator.knowledge.global.security.oauth2.dto.GoogleUserInfoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class GoogleAuthClient {

    private final RestTemplate restTemplate = new RestTemplate(); // 통신 도구

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

    public GoogleTokenResponse getGoogleAccessToken(String code) {
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

        return response.getBody();
    }

    public GoogleUserInfoDto getGoogleUserInfo(GoogleTokenResponse accessTokenResponse) {
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

        return response.getBody();
    }
}
