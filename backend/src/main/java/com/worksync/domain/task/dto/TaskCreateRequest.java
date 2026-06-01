package com.worksync.domain.task.dto;

import com.worksync.domain.employee.entity.JobGrade;
import com.worksync.domain.task.entity.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class TaskCreateRequest {

    @NotBlank
    @Size(max=30, message = "제목은 30자 이내로 작성해주세요")
    private String title;


    private String description;
    private TaskStatus status;
    private Long assigneeId;
    private Long departmentId;




    private JobGrade assigneeJobGrade;


    @Min(0) @Max(100)
    private Integer progress;

    private LocalDate startDate;
    private LocalDate dueDate;
}
