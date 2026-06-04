package com.worksync.domain.file.controller;

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

  // 파일 업로드
  @PostMapping("/upload")
  public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @RequestParam("file") MultipartFile file,
          @RequestParam("refType") String refType) {
    return ResponseEntity.status(201)
            .body(ApiResponse.created(fileService.upload(file, userDetails.getId(), refType)));
  }

  // 파일 단건 조회
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<FileUploadResponse>> findById(@PathVariable("id") Long id) {
    return ResponseEntity.ok(ApiResponse.ok(fileService.findFileId(id)));
  }

  // 첨부 위치별 파일 목록 조회
  @GetMapping
  public ResponseEntity<ApiResponse<List<FileUploadResponse>>> findByRef(
          @RequestParam("refType") String refType,
          @RequestParam("refId") Long refId) {
    return ResponseEntity.ok(ApiResponse.ok(fileService.findByRef(refType, refId)));
  }

  // 파일 삭제
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
    fileService.delete(id);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }
}
