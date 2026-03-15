package com.project.knowledge.global.security.oauth2;

import com.project.knowledge.domain.user.entity.Role;
import com.project.knowledge.domain.user.entity.User;
import com.project.knowledge.domain.user.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    // 롬복(@RequiredArgsConstructor) 대신 순수 자바 생성자 주입!
    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 구글 서버에 통신을 보내서 유저 정보를 받아옵니다. (이건 스프링이 다 해줍니다)
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 2. 구글이 준 데이터 뭉치를 Map 형태로 꺼냅니다.
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 3. 우리가 필요한 알맹이(이메일, 이름, 프사)만 쏙쏙 뽑아냅니다.
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        // 4. 우리 DB에 저장하거나 업데이트하는 커스텀 로직 실행
        User user = saveOrUpdate(email, name, picture);

        // 5. 아까 만든 '포장지'에 DB에서 꺼낸 유저와 구글 데이터를 담아서 시큐리티에게 반환!
        return new CustomOAuth2User(user, attributes);
    }

    // 💡 DB 처리용 헬퍼 메서드 (가입된 사람이면 업데이트, 처음 온 사람이면 저장)
    private User saveOrUpdate(String email, String name, String picture) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            // 이미 가입된 유저라면? 구글 이름이나 프사가 바뀌었을 수 있으니 최신화 해줍니다.
            User existingUser = optionalUser.get();
            existingUser.update(name, picture); // User 엔티티에 이 메서드가 있어야 합니다!
            return userRepository.save(existingUser);
        } else {
            // 완전 처음 온 유저라면? 새 객체를 만들어서 DB에 Insert!
            User newUser = new User(email, name, picture, Role.USER);
            return userRepository.save(newUser);
        }
    }
}
