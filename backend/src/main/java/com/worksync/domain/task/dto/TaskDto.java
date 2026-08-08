// 업무 도메인 요청/응답 DTO 모음
package com.worksync.domain.task.dto;

import com.worksync.domain.employee.entity.JobGrade;
import com.worksync.domain.file.dto.FileDto;
import com.worksync.domain.task.entity.Task;
import com.worksync.domain.task.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TaskDto {

    @Getter @Setter
    @Schema(name = "TaskDto.CreateRequest")
    public static class CreateRequest {

        @NotBlank
        @Size(max=30, message = "제목은 30자 이내로 작성해주세요")
        @Schema(description = "제목", maxLength = 30, example = "주간 보고서 작성")
        private String title;

        @Schema(description = "설명", example = "이번 주 진행사항 정리")
        private String description;

        @Schema(description = "상태 (TODO, IN_PROGRESS, DONE)", example = "TODO")
        private TaskStatus status;

        @Schema(description = "담당자 ID", example = "1")
        private Long assigneeId;

        @Schema(description = "부서 ID", example = "1")
        private Long departmentId;

        @Schema(description = "담당자 직급 (STAFF, SENIOR, ASSISTANT_MANAGER, MANAGER, GENERAL_MANAGER, DIRECTOR, CEO)", example = "STAFF")
        private JobGrade assigneeJobGrade;

        @Min(0) @Max(100)
        @Schema(description = "진행률(%)", example = "50")
        private Integer progress;

        @Schema(description = "시작일", format = "yyyy-MM-dd", example = "2026-08-01")
        private LocalDate startDate;

        @Schema(description = "마감일", format = "yyyy-MM-dd", example = "2026-08-15")
        private LocalDate dueDate;
    }

    @Getter @Builder(toBuilder = true)  //toBuilder는 기존 객체 복사해서 새로 만드는 매서드
    @Schema(name = "TaskDto.Response")
    public static class Response {

        @Schema(description = "업무 ID", example = "1")
        private Long id;

        @Schema(description = "생성자 ID", example = "1")
        private Long creatorId;

        @Schema(description = "생성자 이름", example = "홍길동")
        private String creatorName;

        @Schema(description = "담당자 ID", example = "1")
        private Long assigneeId;

        @Schema(description = "담당자 이름", example = "김철수")
        private String assigneeName;

        @Schema(description = "담당자 직급 (STAFF, SENIOR, ASSISTANT_MANAGER, MANAGER, GENERAL_MANAGER, DIRECTOR, CEO)", example = "STAFF")
        private String assigneeJobGrade;

        @Schema(description = "부서 ID", example = "1")
        private Long departmentId;

        @Schema(description = "부서명", example = "개발팀")
        private String departmentName;

        @Schema(description = "제목", example = "주간 보고서 작성")
        private String title;

        @Schema(description = "설명", example = "이번 주 진행사항 정리")
        private String description;

        @Schema(description = "상태 (TODO, IN_PROGRESS, DONE)", example = "TODO")
        private TaskStatus status;

        @Schema(description = "진행률(%)", example = "50")
        private Integer progress;

        @Schema(description = "시작일", format = "yyyy-MM-dd", example = "2026-08-01")
        private LocalDate startDate;

        @Schema(description = "마감일", format = "yyyy-MM-dd", example = "2026-08-15")
        private LocalDate dueDate;

        @Schema(description = "완료 일시")
        private LocalDateTime completedAt;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        @Schema(description = "수정 일시")
        private LocalDateTime updatedAt;

        @Schema(description = "첨부파일 목록")
        private List<FileDto.UploadResponse> attachments;

        public static Response from(Task task) {
            return Response.builder()
                    .id(task.getId())
                    .creatorId(task.getCreator().getId())
                    .creatorName(task.getCreator().getName())
                    .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                    .assigneeName(task.getAssignee() != null ? task.getAssignee().getName() : null)
                    .assigneeJobGrade(task.getAssignee() !=null ? task.getAssignee().getJobGrade().name():null)
                    .departmentId(task.getDepartment() != null ? task.getDepartment().getId() : null)
                    .departmentName(task.getDepartment() != null ? task.getDepartment().getName() : null)
                    .title(task.getTitle())
                    .description(task.getDescription())
                    .status(task.getStatus())
                    .progress(task.getProgress())
                    .startDate(task.getStartDate())
                    .dueDate(task.getDueDate())
                    .completedAt(task.getCompletedAt())
                    .createdAt(task.getCreatedAt())
                    .updatedAt(task.getUpdatedAt())
                    .build();

        }

        public static Response from(Task task,List<FileDto.UploadResponse>attachments){
            return from(task).toBuilder()
                .attachments(attachments)
                    .build();}
    }

    @Getter @Setter
    @Schema(name = "TaskDto.UpdateRequest")
    public static class UpdateRequest {

        @Size(max=30,message = "제목은 30자 이내로 작성해주세요")
        @Schema(description = "제목", maxLength = 30, example = "주간 보고서 작성(수정)")
        private String title;

        @Schema(description = "설명", example = "이번 주 진행사항 정리(수정)")
        private String description;

        @Schema(description = "담당자 ID", example = "1")
        private Long assigneeId;

        @Schema(description = "부서 ID", example = "1")
        private Long departmentId;

        @Schema(description = "상태 (TODO, IN_PROGRESS, DONE)", example = "IN_PROGRESS")
        private TaskStatus status;

        @Min(0) @Max(100)
        @Schema(description = "진행률(%)", example = "80")
        private Integer progress;

        @Schema(description = "시작일", format = "yyyy-MM-dd", example = "2026-08-01")
        private LocalDate startDate;

        @Schema(description = "마감일", format = "yyyy-MM-dd", example = "2026-08-15")
        private LocalDate dueDate;
    }
}
