package com.worksync.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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
                                .bearerFormat("JWT")))
                .tags(List.of(
                        new Tag().name("Auth").description("인증 API"),
                        new Tag().name("Employee").description("직원 관리 API"),
                        new Tag().name("Department").description("부서 관리 API"),
                        new Tag().name("Attendance").description("근태 관리 API"),
                        new Tag().name("Leave").description("휴가 API"),
                        new Tag().name("Approval").description("전자결재 API"),
                        new Tag().name("Board").description("게시판 API"),
                        new Tag().name("Post").description("게시글 API"),
                        new Tag().name("Task").description("업무 관리 API"),
                        new Tag().name("Chat").description("채팅 API"),
                        new Tag().name("Notification").description("알림 API"),
                        new Tag().name("Dashboard").description("대시보드 요약 API"),
                        new Tag().name("File").description("파일 업로드 API"),
                        new Tag().name("AuditLog").description("감사 로그 API (ADMIN 전용)")
                ));
    }
}
