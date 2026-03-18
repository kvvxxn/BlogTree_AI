package com.navigator.knowledge.domain.user.entity;

public enum Role {
    GUEST("ROLE_GUEST", "손님"),
    USER("ROLE_USER", "일반 사용자"),
    ADMIN("ROLE_ADMIN", "관리자");

    private final String key;
    private final String title;

    // 1. @RequiredArgsConstructor 가 숨어서 만들어주던 '생성자'
    Role(String key, String title) {
        this.key = key;
        this.title = title;
    }

    // 2. @Getter 가 숨어서 만들어주던 'Getter 메서드'
    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }
}