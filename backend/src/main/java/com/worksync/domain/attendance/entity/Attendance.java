package com.worksync.domain.attendance.entity;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "attendance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "work_date"})
)
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDt;

    @Column(name = "check_in_time")
    private LocalDateTime checkedInAt;

    @Column(name = "check_out_time")
    private LocalDateTime checkedOutAt;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "attendance_status_type")
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.NORMAL;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    // 정규 퇴근 시각 — 이보다 일찍 퇴근하면 조퇴
    private static final int WORK_END_HOUR = 18;
    // 정규 출근 시각 — 이 시각부터는 지각
    private static final int WORK_START_HOUR = 9;

    // 퇴근 처리 — 정상 출근자가 18시 전에 퇴근하면 조퇴로 판정 (지각은 지각 그대로 둔다)
    public void checkOut(LocalDateTime checkOutTime){
        this.checkedOutAt = checkOutTime;
        if (this.status == AttendanceStatus.NORMAL && checkOutTime.getHour() < WORK_END_HOUR) {
            this.status = AttendanceStatus.EARLY_LEAVE;
        }
    }

    // 근무 재개 — 같은 날 다시 로그인하면 퇴근 기록을 지우고 출근 시각 기준 상태로 되돌린다
    // (로그아웃이 곧 퇴근 처리라, 점심에 로그아웃했다가 돌아온 사람이 조퇴로 남지 않게 한다)
    public void reopen() {
        this.checkedOutAt = null;
        if (this.status == AttendanceStatus.EARLY_LEAVE) {
            this.status = this.checkedInAt != null && this.checkedInAt.getHour() >= WORK_START_HOUR
                    ? AttendanceStatus.LATE : AttendanceStatus.NORMAL;
        }
    }

    // 관리자 수정 — 출퇴근 시각과 상태를 직접 지정
    public void adminUpdate(LocalDateTime checkedInAt, LocalDateTime checkedOutAt, AttendanceStatus status) {
        this.checkedInAt = checkedInAt;
        this.checkedOutAt = checkedOutAt;
        this.status = status;
    }
}
