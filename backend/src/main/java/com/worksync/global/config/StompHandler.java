package com.worksync.global.config;

import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

// WebSocket 연결시 토큰 확인 -> 이 연결이 누구인지 등록하는 클래스
// 특정 사용자에게만 필요할 때 사용자 식별 필요
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {
    private final JwtTokenProvider jwtTokenProvider;
    private final EmployeeRepository employeeRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // WebSocket 메시지에서 헤더 정보 꺼내기
        // getAccessor() : 원본 메시지의 accessor 참조
        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // 최초 연결 시 한번만 토큰 확인
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 헤더에서 토큰 꺼냄
            String token = accessor.getFirstNativeHeader("Authorization");

            // 순수 토큰만 추출
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 토큰 유효성 검사
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // 토큰에서 사용자 ID 추출
                Long employeeId = jwtTokenProvider.getEmployeeId(token);

                // Authentication 객체 생성
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        String.valueOf(employeeId),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

                // webSocket 연결에 사용자 등록
                accessor.setUser(auth);
            }
        }

        return message;
    }
}
