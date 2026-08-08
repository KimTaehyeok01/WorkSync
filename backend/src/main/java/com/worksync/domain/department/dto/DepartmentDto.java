// 부서 도메인 요청/응답 DTO 모음
package com.worksync.domain.department.dto;

import com.worksync.domain.department.entity.Department;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class DepartmentDto {

    @Getter @Setter
    @Schema(name = "DepartmentDto.Request")
    public static class Request {

        @NotBlank
        @Size(max = 30)
        @Schema(description = "부서명", maxLength = 30, example = "개발팀")
        private String name;
    }

    @Getter @Builder
    @Schema(name = "DepartmentDto.Response")
    public static class Response {

        @Schema(description = "부서 ID", example = "1")
        private Long id;

        @Schema(description = "부서명", example = "개발팀")
        private String name;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        public static Response from(Department department) {
            return Response.builder()
                    .id(department.getId())
                    .name(department.getName())
                    .createdAt(department.getCreatedAt())
                    .build();
        }
    }
}
