package com.worksync.domain.task.repository;

import com.worksync.domain.task.entity.Task;
import com.worksync.domain.task.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task,Long> {


    //전체 목록 -상태 필터 +작업명/담당자명 키워드 검색+페이징
    //작업명,담당자명 키워드 검색을 위해서 @Query사용
    //관리자는 전체, 그 외에는 내 부서 업무이거나 내가 등록·담당한 업무만
    @Query("""
        SELECT t FROM Task t
        LEFT JOIN FETCH t.creator
        LEFT JOIN FETCH t.assignee
        LEFT JOIN FETCH t.department
        WHERE (CAST(:status AS string) IS NULL OR t.status = :status)
          AND (CAST(:keyword AS string) IS NULL
               OR t.title LIKE %:keyword%
               OR t.assignee.name LIKE %:keyword%)
          AND (:isAdmin = true
               OR t.department.id = :departmentId
               OR t.creator.id = :employeeId
               OR t.assignee.id = :employeeId)
        """)

    Page<Task> findAllWithFilter(
            @Param("status")TaskStatus status,
            @Param("keyword")String keyword,
            @Param("isAdmin")boolean isAdmin,
            @Param("departmentId")Long departmentId,
            @Param("employeeId")Long employeeId,
            Pageable pageable);

    //담당자별 목록- 상태 필터+페이징 (조회 권한 조건은 전체 목록과 동일)
    @Query("""
        SELECT t FROM Task t
        LEFT JOIN FETCH t.creator
        LEFT JOIN FETCH t.assignee
        LEFT JOIN FETCH t.department
        WHERE t.assignee.id = :assigneeId
          AND (CAST(:status AS string) IS NULL OR t.status = :status)
          AND (:isAdmin = true
               OR t.department.id = :departmentId
               OR t.creator.id = :employeeId
               OR t.assignee.id = :employeeId)
        """)

    Page<Task> findByAssigneeWithFilter(@Param("assigneeId")Long assigneeId,
                                        @Param("status")TaskStatus status,
                                        @Param("isAdmin")boolean isAdmin,
                                        @Param("departmentId")Long departmentId,
                                        @Param("employeeId")Long employeeId,
                                        Pageable pageable);

    //내가 만든 업무-페이징
    Page<Task> findByCreatorId(Long creatorId, Pageable pageable);

    //부서별 목록- 상태 필터+페이징 (조회 권한 조건은 전체 목록과 동일)
    //departmentId 는 조회할 부서, myDepartmentId 는 로그인한 직원의 부서
    @Query("""
        SELECT t FROM Task t
        LEFT JOIN FETCH t.creator
        LEFT JOIN FETCH t.assignee
        LEFT JOIN FETCH t.department
        WHERE t.department.id = :departmentId
          AND (CAST(:status AS string) IS NULL OR t.status = :status)
          AND (:isAdmin = true
               OR t.department.id = :myDepartmentId
               OR t.creator.id = :employeeId
               OR t.assignee.id = :employeeId)
        """)

    Page<Task> findByDepartmentWithFilter(
            @Param("departmentId")Long departmentId,
            @Param("status")TaskStatus status,
            @Param("isAdmin")boolean isAdmin,
            @Param("myDepartmentId")Long myDepartmentId,
            @Param("employeeId")Long employeeId,
            Pageable pageable);


    // 통합 검색 — 제목이 일치하는 업무 중 볼 수 있는 것
    // 관리자는 전체, 그 외에는 내 부서 업무이거나 내가 등록·담당한 업무
    @Query("""
        SELECT t FROM Task t
        LEFT JOIN FETCH t.assignee
        WHERE t.title LIKE CONCAT('%', :keyword, '%')
          AND (:isAdmin = true
               OR t.department.id = :departmentId
               OR t.creator.id = :employeeId
               OR t.assignee.id = :employeeId)
        ORDER BY t.createdAt DESC
        """)
    List<Task> searchVisible(@Param("keyword") String keyword,
                             @Param("isAdmin") boolean isAdmin,
                             @Param("departmentId") Long departmentId,
                             @Param("employeeId") Long employeeId,
                             Pageable pageable);

    //dashboard용
    long countByAssigneeIdAndStatus(Long assigneeId,TaskStatus status);
    List<Task> findAllByAssigneeId(Long assigneeId);

}
