package com.navigator.knowledge.global.security.oauth2;

import com.navigator.knowledge.global.security.oauth2.dto.GoogleTokenResponse;
import com.navigator.knowledge.global.security.oauth2.dto.GoogleUserInfoDto;
import com.navigator.knowledge.global.security.oauth2.exception.GoogleOAuthException;
import com.navigator.knowledge.global.security.oauth2.properties.GoogleOAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(GoogleOAuthProperties.class)
public class GoogleAuthClient {

    private final RestTemplate restTemplate;
    private final GoogleOAuthProperties googleOAuthProperties;

    // 왜 구글 서버 리프레시 토큰은 없는가?
    // 최초 사용자 인증 이후에는 구글 서버와 통신할 일이 없기 때문
    public GoogleTokenResponse getGoogleAccessToken(String code, String redirectUri) {
        // 인가 코드가 제대로 왔는지 확인
        if (code == null || code.isBlank()) {
            log.warn("Google access token code is null or empty");
            throw new GoogleOAuthException("Google 로그인 요청에 인가 코드가 없습니다. 다시 로그인해 주세요.");
        }

        String resolvedRedirectUri = StringUtils.hasText(redirectUri)
                ? redirectUri
                : googleOAuthProperties.redirectUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 구글이 요구하는 필수 파라미터들 뭉치기
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleOAuthProperties.clientId());
        params.add("client_secret", googleOAuthProperties.clientSecret());
        params.add("redirect_uri", resolvedRedirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        log.info("[Google OAuth] Requesting Access Token... (tokenUri: {})", googleOAuthProperties.tokenUri());

        ResponseEntity<GoogleTokenResponse> response;
        try {
            response = restTemplate.postForEntity(googleOAuthProperties.tokenUri(), request, GoogleTokenResponse.class);
        } catch (RestClientResponseException e) {
            log.warn(
                    "[Google OAuth] Access token request failed. status={}, response={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString()
            );
            throw new GoogleOAuthException("Google 로그인 요청이 만료되었거나 이미 사용되었습니다. 다시 로그인해 주세요.", e);
        } catch (RestClientException e) {
            log.warn("[Google OAuth] Access token request failed.", e);
            throw new GoogleOAuthException("Google 로그인 처리 중 외부 인증 요청에 실패했습니다. 다시 시도해 주세요.", e);
        }

        GoogleTokenResponse responseBody = response.getBody();

        // 인가 코드가 인자로 넘어왔다 -> 이미 회원이다
        // 하지만 구글 서버 자체의 오류로 인해 null이 될 수 있음
        if (responseBody == null) {
            log.error("[Google OAuth] Google Access Token response is null");
            throw new GoogleOAuthException("Google 로그인 처리 중 토큰 응답이 비어있습니다. 다시 시도해 주세요.");
        }

        log.info("[Google OAuth] Google Access Token request successful");
        return responseBody;
    }

    public GoogleUserInfoDto getGoogleUserInfo(GoogleTokenResponse accessTokenResponse) {
        if (accessTokenResponse == null || accessTokenResponse.getAccessToken() == null) {
            log.warn("Google access token response is null or empty");
            throw new GoogleOAuthException("유효하지 않은 Google 액세스 토큰입니다. 다시 로그인해 주세요.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessTokenResponse.getAccessToken());

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<GoogleUserInfoDto> response;
        try {
            response = restTemplate.exchange(
                    googleOAuthProperties.userInfoUri(),
                    HttpMethod.GET,
                    request,
                    GoogleUserInfoDto.class
            );
        } catch (RestClientResponseException e) {
            log.warn(
                    "[Google OAuth] UserInfo request failed. status={}, response={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString()
            );
            throw new GoogleOAuthException("Google 사용자 정보를 가져오지 못했습니다. 다시 로그인해 주세요.", e);
        } catch (RestClientException e) {
            log.warn("[Google OAuth] UserInfo request failed.", e);
            throw new GoogleOAuthException("Google 사용자 정보 요청에 실패했습니다. 다시 시도해 주세요.", e);
        }

        GoogleUserInfoDto userInfo = response.getBody();
        if (userInfo == null) {
            log.error("[Google OAuth] Google User Info response is null");
            throw new GoogleOAuthException("Google 사용자 정보 응답이 비어있습니다. 다시 시도해 주세요.");
        }

        log.info("[Google OAuth] Google UserInfo request successful");

        return userInfo;
    }
}
