package com.worksync.global.config;

import com.worksync.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

// WebSocket 연결시 토큰 확인 -> 이 연결이 누구인지 등록하는 클래스
// 특정 사용자에게만 필요할 때 사용자 식별 필요
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // WebSocket 메시지에서 헤더 정보 꺼내기
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // CONNECT 명령일 때만 실행 (최초 연결 시 한번만 토큰 확인)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 프론트에서 보낸 Authorization 헤더에서 토큰 꺼내기
            String token = accessor.getFirstNativeHeader("Authorization");

            // 토큰이 있고, Bearer 로 시작하면 유효한 토큰
            if (token != null && token.startsWith("Bearer ")) {
                // 순수 토큰 값 추출
                token = token.substring(7);
                // 토큰에서 userId 추출
                Long userId = jwtTokenProvider.getEmployeeId(token);
                System.out.println("등록" + userId);
                accessor.setUser(() -> String.valueOf(userId));
            }
        }

        return message;
    }
}
