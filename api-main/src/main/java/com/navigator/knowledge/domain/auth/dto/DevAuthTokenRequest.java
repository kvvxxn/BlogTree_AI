package com.navigator.knowledge.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DevAuthTokenRequest {

    private String email;
    private String name;
    private String careerGoal;
}
