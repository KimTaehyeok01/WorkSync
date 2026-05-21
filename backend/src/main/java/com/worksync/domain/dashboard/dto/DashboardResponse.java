package com.worksync.domain.dashboard.dto;

import com.worksync.domain.attendance.entity.Attendance;
import com.worksync.domain.employee.entity.Employee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private int pendingApprovals;
    private TaskSummary taskSummary;
    private AttendanceSummary attendance;
    private List<RecentPost> recentPosts;
    private List<TeamMemberStatus> teamMemberStatus;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskSummary {
        private int todo;
        private int inProgress;
        private int done;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceSummary {
        private LocalDateTime checkInTime;
        private LocalDateTime checkOutTime;
        private Attendance.Status status;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentPost {
        private Long id;
        private String title;
        private String boardName;
        private String author;
        private LocalDateTime createdAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamMemberStatus {
        private Long id;
        private String name;
        private String profileImage;
        private Employee.Status status;
        private Employee.JobGrade jobGrade;
    }
}
