package com.worksync.domain.leave.entity;

import com.worksync.domain.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "annual_leave_balance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "year"})
)
@EntityListeners(AuditingEntityListener.class)
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnnualLeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, columnDefinition = "SMALLINT")
    private Short year;

    @Column(name = "total_days", nullable = false, precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal totalDays = BigDecimal.ZERO;

    @Column(name = "used_days", nullable = false, precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal usedDays = BigDecimal.ZERO;

    @Column(name = "pending_days", nullable = false, precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal pendingDays = BigDecimal.ZERO;

    // JPA가 자동으로 동시 저장 충돌 감지
    // OptimisticLockingFailureException 발생하고 트랜잭션이 롤백
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void useLeave(BigDecimal days){
        this.usedDays=this.usedDays.add(days);
    }

    public BigDecimal getRemainingDays(){
        return this.totalDays.subtract(this.usedDays).subtract(this.pendingDays);
    }
    public void addPendingDays(BigDecimal days) {
        this.pendingDays = this.pendingDays.add(days);
    }
    public void subtractPendingDays(BigDecimal days){
        this.pendingDays = this.pendingDays.subtract(days);
    }
}
