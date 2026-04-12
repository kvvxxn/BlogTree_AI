package com.navigator.knowledge.domain.user.service;

import com.navigator.knowledge.domain.user.dto.UserProfileResponse;
import com.navigator.knowledge.domain.user.dto.UserProfileUpdateRequest;
import com.navigator.knowledge.domain.user.entity.Role;
import com.navigator.knowledge.domain.user.entity.User;
import com.navigator.knowledge.domain.user.exception.UserNotFoundException;
import com.navigator.knowledge.domain.user.repository.UserRepository;
import com.navigator.knowledge.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("내 프로필 조회 시 사용자 정보를 반환한다")
    void getMyProfile_returnsUserProfile() {
        User user = createUser(1L, "old name", "career");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserProfileResponse response = userService.getMyProfile(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getName()).isEqualTo("old name");
        assertThat(response.getCareerGoal()).isEqualTo("career");
    }

    @Test
    @DisplayName("내 프로필 수정 시 이름과 커리어 목표를 변경한다")
    void updateMyProfile_updatesFields() throws Exception {
        User user = createUser(1L, "old name", "career");
        UserProfileUpdateRequest request = createUpdateRequest("new name", "new career");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserProfileResponse response = userService.updateMyProfile(1L, request);

        assertThat(response.getName()).isEqualTo("new name");
        assertThat(response.getCareerGoal()).isEqualTo("new career");
        assertThat(user.getName()).isEqualTo("new name");
        assertThat(user.getCareerGoal()).isEqualTo("new career");
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 프로필 조회 시 예외가 발생한다")
    void getMyProfile_throwsWhenUserNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("userId=99");
    }

    @Test
    @DisplayName("careerGoal 조회 시 설정된 값을 반환한다")
    void getRequiredCareerGoal_returnsCareerGoal() {
        User user = createUser(1L, "old name", "backend developer");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        String careerGoal = userService.getRequiredCareerGoal(1L);

        assertThat(careerGoal).isEqualTo("backend developer");
    }

    @Test
    @DisplayName("careerGoal이 비어 있으면 예외가 발생한다")
    void getRequiredCareerGoal_throwsWhenCareerGoalMissing() {
        User user = createUser(1L, "old name", null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getRequiredCareerGoal(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("careerGoal이 설정되지 않았습니다.");
    }

    private User createUser(Long id, String name, String careerGoal) {
        return User.builder()
                .id(id)
                .email("user@example.com")
                .name(name)
                .profileImageUrl("https://example.com/profile.png")
                .role(Role.USER)
                .careerGoal(careerGoal)
                .build();
    }

    private UserProfileUpdateRequest createUpdateRequest(String name, String careerGoal) throws Exception {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        setField(request, "name", name);
        setField(request, "careerGoal", careerGoal);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
