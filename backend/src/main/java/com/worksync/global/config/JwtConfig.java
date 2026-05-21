package com.worksync.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter @Setter

// 서버와 클라이언트 간의 안전한 통신과 사용자 인증을 위해 사용하는 JWT 관련 설정 클래스
public class JwtConfig {

    private String secret;
    private String issuer;
    private long accessTokenExpMinutes = 60;
    private long refreshTokenExpDays = 7;
}
