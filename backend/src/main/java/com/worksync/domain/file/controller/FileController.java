package com.worksync.domain.file.controller;

import com.worksync.domain.file.dto.FileSaveRequest;
import com.worksync.domain.file.dto.FileUploadResponse;
import com.worksync.domain.file.service.FileService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

  private final FileService fileService;

  // 파일 스토리지 저장
  @PostMapping("/upload")
  public ResponseEntity<ApiResponse<FileUploadResponse>> upload(@RequestParam MultipartFile file) {
    return ResponseEntity.status(201)
            .body(ApiResponse.created(fileService.upload(file)));
  }

  // 파일 DB 저장
  @PostMapping("/save")
  public ResponseEntity<ApiResponse<Void>> saveFile(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @RequestBody FileSaveRequest request) {
    fileService.updateRefId(userDetails.getId(), request);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }

  // 파일 단건 조회
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<FileUploadResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(fileService.findFileId(id)));
  }

  // 첨부 위치별 파일 목록 조회
  @GetMapping
  public ResponseEntity<ApiResponse<List<FileSaveRequest>>> findByRef(
          @RequestParam("refType") String refType,
          @RequestParam("refId") Long refId) {
    return ResponseEntity.ok(ApiResponse.ok(fileService.findByRef(refType, refId)));
  }

  // 파일 DB 삭제
  @DeleteMapping("/delete")
  public ResponseEntity<ApiResponse<Void>> deleteFile(@RequestParam("refType") String refType, @RequestParam("refId") Long refId) {
    fileService.deleteFile(refType, refId);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }
}
