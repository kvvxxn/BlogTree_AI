package com.navigator.knowledge.domain.user.service;

import com.navigator.knowledge.domain.user.dto.UserProfileResponse;
import com.navigator.knowledge.domain.user.dto.UserProfileUpdateRequest;
import com.navigator.knowledge.domain.user.entity.User;
import com.navigator.knowledge.domain.user.exception.UserNotFoundException;
import com.navigator.knowledge.domain.user.repository.UserRepository;
import com.navigator.knowledge.global.exception.BusinessException;
import com.navigator.knowledge.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String JPA_TRANSACTION_MANAGER = "jpaTransactionManager";

    private final UserRepository userRepository;

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER, readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = getUser(userId);
        return UserProfileResponse.from(user);
    }

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER)
    public UserProfileResponse updateMyProfile(Long userId, UserProfileUpdateRequest request) {
        User user = getUser(userId);
        user.updateName(request.getName());
        user.updateCareerGoal(request.getCareerGoal());
        return UserProfileResponse.from(user);
    }

    @Transactional(transactionManager = JPA_TRANSACTION_MANAGER, readOnly = true)
    public String getRequiredCareerGoal(Long userId) {
        User user = getUser(userId);
        if (!StringUtils.hasText(user.getCareerGoal())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "careerGoal이 설정되지 않았습니다.");
        }
        return user.getCareerGoal();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
