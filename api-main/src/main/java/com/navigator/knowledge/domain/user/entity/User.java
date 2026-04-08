package com.navigator.knowledge.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
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

    @Column(length = 1000)
    private String careerGoal;

    // 구글 정보가 업데이트되었을 때 갱신용
    public User update(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        return this;
    }

    public void updateCareerGoal(String careerGoal) {
        this.careerGoal = careerGoal;
    }
}