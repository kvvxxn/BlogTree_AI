package com.project.knowledge.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@Entity
@Table(name = "users") // DB에 생성될 테이블 이름 (user는 예약어라 users로 씁니다)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK (자동 증가)

    @Column(nullable = false, unique = true)
    private String email; // 구글 이메일

    @Column(nullable = false)
    private String name; // 구글 닉네임 (또는 실명)

    private String profileImageUrl; // 구글 프로필 사진 URL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // 아까 만든 권한 (GUEST, USER, ADMIN)

    // 1. JPA가 테이블을 만들 때 필요한 '기본 생성자' (롬복 @NoArgsConstructor 역할)
    protected User() {
    }

    // 2. 유저를 처음 생성할 때 쓸 '생성자' (롬복 @Builder 역할 대체)
    public User(String email, String name, String profileImageUrl, Role role) {
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
    }

    // 3. 변수값을 가져오기 위한 Getter 메서드들 (롬복 @Getter 역할)
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public Role getRole() {
        return role;
    }

    // 4. 구글 정보가 업데이트되었을 때 갱신용
    public User update(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        return this;
    }
}