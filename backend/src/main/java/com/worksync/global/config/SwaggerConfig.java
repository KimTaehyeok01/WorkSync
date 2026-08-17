package com.worksync.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtScheme = "BearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("WorkSync API")
                        .description("WorkSync 그룹웨어 API 명세서")
                        .version("v1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(jwtScheme))
                .components(new Components()
                        .addSecuritySchemes(jwtScheme, new SecurityScheme()
                                .name(jwtScheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("01. 인증")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi hrApi() {
        return GroupedOpenApi.builder()
                .group("02. 직원·부서·근태·휴가")
                .pathsToMatch("/api/employees/**", "/api/departments/**", "/api/attendance/**", "/api/leave/**")
                .build();
    }

    @Bean
    public GroupedOpenApi workApi() {
        return GroupedOpenApi.builder()
                .group("03. 업무관리·전자결재")
                .pathsToMatch("/api/tasks/**", "/api/approvals/**")
                .build();
    }

    @Bean
    public GroupedOpenApi boardApi() {
        return GroupedOpenApi.builder()
                .group("04. 게시판")
                .pathsToMatch("/api/boards/**")
                .build();
    }

    @Bean
    public GroupedOpenApi chatApi() {
        return GroupedOpenApi.builder()
                .group("05. 채팅·알림")
                .pathsToMatch("/api/chat/**", "/api/notifications/**")
                .build();
    }

    @Bean
    public GroupedOpenApi dashboardApi() {
        return GroupedOpenApi.builder()
                .group("06. 대시보드")
                .pathsToMatch("/api/dashboard/**")
                .build();
    }

    @Bean
    public GroupedOpenApi fileApi() {
        return GroupedOpenApi.builder()
                .group("07. 파일")
                .pathsToMatch("/api/files/**")
                .build();
    }

    @Bean
    public GroupedOpenApi auditApi() {
        return GroupedOpenApi.builder()
                .group("08. 감사로그")
                .pathsToMatch("/api/audit-logs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("전체")
                .pathsToMatch("/api/**")
                .build();
    }
}
