package com.navigator.knowledge.global.security.oauth2.dto;

// JSON 파일을 받아서 객체 변수로 넣어주는 어노테이션. 한번 세팅하면 값 변경 불가
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleUserInfoDto {

    @JsonProperty
    private String id; // 구글 내부 고유 ID

    @JsonProperty
    private String email;

    @JsonProperty("verified_email")
    private Boolean verifiedEmail;

    @JsonProperty
    private String name;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty
    private String picture; // 프로필 이미지 URL

    @JsonProperty
    private String locale;
}
