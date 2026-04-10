package com.navigator.knowledge.domain.user.presentation;

import com.navigator.knowledge.domain.user.dto.UserProfileResponse;
import com.navigator.knowledge.domain.user.dto.UserProfileUpdateRequest;
import com.navigator.knowledge.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal Long userId) {
        UserProfileResponse response = userService.getMyProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        UserProfileResponse response = userService.updateMyProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}
