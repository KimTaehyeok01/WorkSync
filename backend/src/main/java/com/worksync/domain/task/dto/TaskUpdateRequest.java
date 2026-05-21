package com.worksync.domain.task.dto;

import com.worksync.domain.task.entity.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class TaskUpdateRequest {

    private String title;
    private String description;
    private Long assigneeId;
    private TaskStatus status;

    @Min(0) @Max(100)
    private Integer progress;

    private LocalDate startDate;
    private LocalDate dueDate;
}
