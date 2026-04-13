package com.navigator.knowledge.domain.user.dto;

import com.navigator.knowledge.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String email;
    private String name;
    private String profileImageUrl;
    private String careerGoal;

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getCareerGoal()
        );
    }
}
