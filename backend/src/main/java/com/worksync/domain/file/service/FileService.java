package com.worksync.domain.file.service;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.file.dto.FileUploadResponse;
import com.worksync.domain.file.entity.FileAttachment;
import com.worksync.domain.file.repository.FileAttachmentRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.EOFException;
import java.io.File;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {
  private final FileAttachmentRepository fileAttachmentRepository;
  private final EmployeeRepository employeeRepository;

  // 파일 저장경로 일단은 나중에 S3로 바꿔야함?
  private static final String UPLOAD_DIR = "uploads/";

  // 파일 업로드
  @Transactional
  public FileUploadResponse upload(MultipartFile file, Long uploaderId, String refType, Long refId) throws EOFException{

    // 업로드 사원 조회
    Employee uploader = employeeRepository.findById(uploaderId)
            .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

    // 업로드 폴더가 없을시 생성
    File uploadDir = new File(UPLOAD_DIR); // 실제파일 만드는거아니고 경로를 가르키는객체
    if (!uploadDir.exists()) uploadDir.mkdirs(); // 해당폴더가 존재하는지 보고 없으면 폴더 생성

    // 파일명 중복방지 UUID가 고유한 파일명을 생성해줌.
    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
    String filePath = UPLOAD_DIR + fileName;

    // 실제 파일 로컬저장
    file.transferTo(new File((filePath)));

    // DB에 파일 정보 저장
    FileAttachment fileAttachment = FileAttachment.builder()
            .uploader(uploader)
            .originalName((file.getOriginalFilename()))
            .filePath(filePath)
            .fileSize(file.getSize())
            .mimeType(file.getContentType())
            .refType(refType)
            .refId(refId)
            .build();
    return FileUploadResponse.from(fileAttachmentRepository.save(fileAttachment));
  }

  //
}
