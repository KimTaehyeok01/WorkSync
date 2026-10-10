package com.worksync.domain.file.repository;

import com.worksync.domain.file.entity.FileAttachment;
import com.worksync.domain.file.entity.RefType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment,Long> {
  // findBy,RefType,And,RefId 자동으로 쿼리 생성
  List<FileAttachment> findByRefTypeAndRefId(RefType refType, Long refId);

  // 같은 스토리지 파일을 가리키는 다른 첨부가 남아 있는지 — 삭제 대상(excludedIds)은 제외하고 확인
  boolean existsByFilePathAndIdNotIn(String filePath, Collection<Long> excludedIds);

  // 첨부가 달린 원글 id 만 한 번에 추린다 — 목록의 클립 아이콘 표시용 (원글마다 조회하는 N+1 방지)
  @Query("SELECT DISTINCT f.refId FROM FileAttachment f WHERE f.refType = :refType AND f.refId IN :refIds")
  List<Long> findRefIdsWithAttachment(@Param("refType") RefType refType, @Param("refIds") Collection<Long> refIds);
}
