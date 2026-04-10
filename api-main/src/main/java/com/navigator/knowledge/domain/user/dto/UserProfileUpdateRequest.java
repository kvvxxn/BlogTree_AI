package com.navigator.knowledge.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserProfileUpdateRequest {

    @NotBlank(message = "name은 비어 있을 수 없습니다.")
    @Size(max = 100, message = "name은 100자를 초과할 수 없습니다.")
    private String name;

    @Size(max = 1000, message = "careerGoal은 1000자를 초과할 수 없습니다.")
    private String careerGoal;
}
