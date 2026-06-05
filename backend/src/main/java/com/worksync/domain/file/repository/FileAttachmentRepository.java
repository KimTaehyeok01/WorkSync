package com.worksync.domain.file.repository;

import com.worksync.domain.file.entity.FileAttachment;
import com.worksync.domain.file.entity.RefType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment,Long> {
  // findBy,RefType,And,RefId 자동으로 쿼리 생성
  List<FileAttachment> findByRefTypeAndRefId(RefType refType, Long refId);
}
