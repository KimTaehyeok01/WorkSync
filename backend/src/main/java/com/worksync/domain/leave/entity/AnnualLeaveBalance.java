package com.worksync.domain.leave.entity;

import com.worksync.domain.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "annual_leave_balance",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "year"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class AnnualLeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Short year;

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal totalDays;  //BigDecimal 소수점 사용을 위해서 사용

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal usedDays;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    public BigDecimal getRemainDays() {
        return totalDays.subtract(usedDays);
    }

    public void deductDays(BigDecimal days) {
        this.usedDays = this.usedDays.add(days);
    }
}
