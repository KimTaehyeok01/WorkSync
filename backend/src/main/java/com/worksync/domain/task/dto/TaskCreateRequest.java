package com.worksync.domain.task.dto;

import com.worksync.domain.task.entity.Task;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCreateRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    private String description;

    @NotNull
    private Long assigneeId;

    private Long departmentId;

    @NotNull
    private Task.Status status;

    @Min(0)
    @Max(100)
    private Integer progress;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate dueDate;
}
