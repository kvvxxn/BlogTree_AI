package com.project.knowledge.domain.user.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Getter
@Setter
@AllArgsConstructor // ⭐ 이게 핵심! 모든 필드(id, email, token)를 받는 생성자를 만듭니다.
@Builder
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 유저의 교환권인지 알아야 하니 이메일을 저장합니다. (중복 불가)
    @Column(nullable = false, unique = true)
    private String email;

    // 발급된 Refresh Token 값 (토큰 길이가 길 수 있으므로 512자로 넉넉하게 잡습니다)
    @Column(nullable = false, length = 512)
    private String token;

    // JPA가 엔티티를 생성할 때 사용하는 기본 생성자 (필수!)
    protected RefreshToken() {}

    public RefreshToken(String email, String token) {
        this.email = email;
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    // 기존에 있던 유저가 로그인해서 토큰이 새로 발급되면, 이 메서드로 값을 덮어씌웁니다.
    public void updateToken(String token) {
        this.token = token;
    }
}
