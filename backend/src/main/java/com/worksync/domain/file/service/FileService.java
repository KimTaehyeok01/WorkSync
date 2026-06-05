package com.worksync.domain.file.service;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.file.dto.FileUploadResponse;
import com.worksync.domain.file.entity.FileAttachment;
import com.worksync.domain.file.entity.RefType;
import com.worksync.domain.file.repository.FileAttachmentRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final FileAttachmentRepository fileAttachmentRepository;
    private final EmployeeRepository employeeRepository;

    private final RestClient restClient = RestClient.create();

    @Value("${app.supabase.url}")
    private String supabaseUrl;

    @Value("${app.supabase.service-role-key}")
    private String serviceRoleKey;

    private static final String BUCKET = "WorkSync";

    // refId를 제외한 초기 파일 업로드
    @Transactional
    public FileUploadResponse upload(MultipartFile file, Long uploaderId, String refType) {

        // 업로드 사원 조회
        Employee uploader = employeeRepository.findById(uploaderId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        // Storage 경로는 UUID만 사용 (한글/특수문자 방지)
        String ext = "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectPath = UUID.randomUUID() + ext;

        // 파일 바이트 추출
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        // Supabase Storage 업로드
        try {
            restClient.post()
                    .uri(supabaseUrl + "/storage/v1/object/" + BUCKET + "/" + objectPath)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .contentType(MediaType.parseMediaType(
                            file.getContentType() != null ? file.getContentType() : "application/octet-stream"))
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("[FileService] Supabase 업로드 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        // 공개 URL 생성
        String publicUrl = supabaseUrl + "/storage/v1/object/public/" + BUCKET + "/" + objectPath;

        // String refType -> RefType refType 타입 변환
        RefType refTypeName = RefType.fromTypeName(refType);

        // DB에 파일 정보 저장
        FileAttachment fileAttachment = FileAttachment.builder()
                .uploader(uploader)
                .originalName(file.getOriginalFilename())
                .filePath(publicUrl)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .refType(refTypeName)
                .refId(null)
                .build();

        return FileUploadResponse.from(fileAttachmentRepository.save(fileAttachment));
    }

    // 최종 저장시 refId 추가
    @Transactional
    public void updateRefId(List<String> fileUrls, Long refId) {
        // 파일 경로가 null이거나 비어있으면 돌려보냄
        if (fileUrls == null || fileUrls.isEmpty()) return;

        // refId 최종 저장
        fileUrls.forEach(url -> {
            FileAttachment file = fileAttachmentRepository.findByFilePath(url).orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));
            file.updateRefId(refId);
        });
    }

    // 파일 단건 조회
    public FileUploadResponse findFileId(Long id) {
        FileAttachment file = fileAttachmentRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));
        return FileUploadResponse.from(file);
    }

    // 첨부 위치별 파일 목록 조회
    public List<FileUploadResponse> findByRef(String refType, Long refId) {
        // String refType -> RefType refType 타입 변환
        RefType refTypeName = RefType.fromTypeName(refType);

        return fileAttachmentRepository.findByRefTypeAndRefId(refTypeName, refId)
                .stream()
                .map(FileUploadResponse::from)
                .collect(Collectors.toList());
    }

    // 파일 삭제
    @Transactional
    public void delete(Long id) {
        FileAttachment file = fileAttachmentRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

        // 저장된 URL에서 objectPath 추출
        String prefix = supabaseUrl + "/storage/v1/object/public/" + BUCKET + "/";
        String objectPath = file.getFilePath().replace(prefix, "");

        // Supabase Storage 삭제
        try {
            restClient.method(HttpMethod.DELETE)
                    .uri(supabaseUrl + "/storage/v1/object/" + BUCKET)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(objectPath)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("[FileService] Storage 삭제 실패 (DB 삭제 진행): {}", e.getMessage());
        }

        // DB에서 파일 정보 삭제
        fileAttachmentRepository.delete(file);
    }
}
