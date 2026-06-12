package com.worksync.domain.task.entity;

import com.worksync.domain.department.entity.Department;
import com.worksync.domain.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "task")
@EntityListeners(AuditingEntityListener.class)
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Employee assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false, length = 30)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "task_status_type")
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Column(nullable = false)
    @Builder.Default
    private Integer progress = 0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(String title,String description,Employee assignee,Department department
            ,TaskStatus status, Integer progress,LocalDate startDate,LocalDate dueDate){
        if(title !=null) this.title=title;
        if(description !=null) this.description=description;
        if(assignee !=null) this.assignee=assignee;
        if(department !=null) this.department=department;
        if(status !=null){
            this.status=status;
            if(status == TaskStatus.DONE && this.completedAt== null){
                this.completedAt=LocalDateTime.now();
            }
        }
        if(progress != null)this.progress=progress;
        if(startDate !=null)this.startDate=startDate;
        if(dueDate !=null) this.dueDate=dueDate;
    }
}
