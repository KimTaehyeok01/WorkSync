package com.worksync.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // 서버가 메시지 라우팅 후 클라이언트에게 전달하는 메세지 브로커의 동작 방식을 설정하는 메서드
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 메시지를 수신할 경로
        registry.enableSimpleBroker("/topic", "/queue");
        // 클라이언트가 메시지를 발송할 경로
        registry.setApplicationDestinationPrefixes("/app");
        // 특정 사용자에게 메시지를 발송할 경로
        registry.setUserDestinationPrefix("/user");
    }

    // 클라이언트가 서버에 접속할 수 있는 웹소켓 연결 엔드포인트(URL)를 설정하는 핵심 메서드
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트 - frontendUrl 외 origin 차단
        registry.addEndpoint("/ws")
                .setAllowedOrigins(frontendUrl)
                .withSockJS(); // SockJS : 안정적인 실시간 양방향 통신을 지원하는 JS 라이브러리
    }
}
// 소희와 아이들 화이팅!