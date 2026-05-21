package com.worksync.domain.task.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class TaskCreateRequest {

    @NotBlank
    private String title;

    private String description;
    private Long assigneeId;
    private Long departmentId;

    @Min(0) @Max(100)
    private Integer progress;

    private LocalDate startDate;
    private LocalDate dueDate;
}
