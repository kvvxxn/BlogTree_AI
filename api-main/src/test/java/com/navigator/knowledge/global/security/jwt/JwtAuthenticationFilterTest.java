package com.navigator.knowledge.global.security.jwt;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("async dispatch에서도 JWT를 다시 읽어 인증 컨텍스트를 세팅한다")
    void doFilter_authenticatesAsyncDispatch() throws Exception {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProvider);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setDispatcherType(DispatcherType.ASYNC);
        request.addHeader("Authorization", "Bearer access-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authentication = new AtomicReference<>();

        when(jwtProvider.validateToken("access-token")).thenReturn(true);
        when(jwtProvider.getCategoryFromToken("access-token")).thenReturn("access");
        when(jwtProvider.getUserIdFromToken("access-token")).thenReturn(1L);
        when(jwtProvider.getRoleFromToken("access-token")).thenReturn("ROLE_USER");

        FilterChain filterChain = (req, res) ->
                authentication.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, filterChain);

        assertThat(authentication.get()).isNotNull();
        assertThat(authentication.get().getPrincipal()).isEqualTo(1L);
        assertThat(authentication.get().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }
}
