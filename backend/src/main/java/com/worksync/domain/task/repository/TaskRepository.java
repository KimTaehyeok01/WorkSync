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
    @Query("""
        SELECT t FROM Task t
        LEFT JOIN FETCH t.creator
        LEFT JOIN FETCH t.assignee
        LEFT JOIN FETCH t.department
        WHERE (CAST(:status AS string) IS NULL OR t.status = :status)
          AND (CAST(:keyword AS string) IS NULL
               OR t.title LIKE %:keyword%
               OR t.assignee.name LIKE %:keyword%)
        """)

    Page<Task> findAllWithFilter(
            @Param("status")TaskStatus status,
            @Param("keyword")String keyword,
            Pageable pageable);

    //담당자별 목록- 상태 필터+페이징
    @Query("""
        SELECT t FROM Task t
        LEFT JOIN FETCH t.creator
        LEFT JOIN FETCH t.assignee
        LEFT JOIN FETCH t.department
        WHERE t.assignee.id = :assigneeId
          AND (CAST(:status AS string) IS NULL OR t.status = :status)
        """)

    Page<Task> findByAssigneeWithFilter(@Param("assigneeId")Long assigneeId,
                                        @Param("status")TaskStatus status,
                                        Pageable pageable);

    //내가 만든 업무-페이징
    Page<Task> findByCreatorId(Long creatorId, Pageable pageable);

    //부서별 목록- 상태 필터+페이징
    @Query("""
        SELECT t FROM Task t
        LEFT JOIN FETCH t.creator
        LEFT JOIN FETCH t.assignee
        LEFT JOIN FETCH t.department
        WHERE t.department.id = :departmentId
          AND (CAST(:status AS string) IS NULL OR t.status = :status)
        """)

    Page<Task> findByDepartmentWithFilter(
            @Param("departmentId")Long departmentId,
            @Param("status")TaskStatus status,
            Pageable pageable);


    //dashboard용
    long countByAssigneeIdAndStatus(Long assigneeId,TaskStatus status);
    List<Task> findAllByAssigneeId(Long assigneeId);

}
