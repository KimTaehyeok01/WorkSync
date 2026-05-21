package com.worksync.domain.task.dto;

import com.worksync.domain.task.entity.Task;
import com.worksync.domain.task.entity.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Builder
public class TaskResponse {

    private Long id;
    private Long creatorId;
    private String creatorName;
    private Long assigneeId;
    private String assigneeName;
    private Long departmentId;
    private String departmentName;
    private String title;
    private String description;
    private TaskStatus status;
    private Integer progress;
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaskResponse from(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .creatorId(task.getCreator().getId())
                .creatorName(task.getCreator().getName())
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getName() : null)
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
}
