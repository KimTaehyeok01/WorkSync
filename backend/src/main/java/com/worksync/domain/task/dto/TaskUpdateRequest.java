package com.worksync.domain.task.dto;

import com.worksync.domain.task.entity.Task;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskUpdateRequest {

    @Size(max = 200)
    private String title;

    private String description;
    private Long assigneeId;
    private Task.Status status;

    @Min(0)
    @Max(100)
    private Integer progress;

    private LocalDate dueDate;
}
