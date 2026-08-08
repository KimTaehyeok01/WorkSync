package com.worksync.domain.file.controller;

import com.worksync.domain.file.dto.FileDto;
import com.worksync.domain.file.service.FileService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "File", description = "파일 업로드 API")
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

  private final FileService fileService;

  // 파일 스토리지 저장
  @Operation(summary = "파일 스토리지 업로드")
  @PostMapping("/upload")
  public ResponseEntity<ApiResponse<FileDto.UploadResponse>> upload(@RequestParam MultipartFile file) {
    return ResponseEntity.status(201)
            .body(ApiResponse.created(fileService.upload(file)));
  }

  // 파일 DB 저장
  @Operation(summary = "파일 메타 정보 DB 저장")
  @PostMapping("/save")
  public ResponseEntity<ApiResponse<FileDto.UploadResponse>> saveFile(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @RequestBody FileDto.SaveRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(fileService.updateRefId(userDetails.getId(), request)));
  }

  // 파일 단건 조회
  @Operation(summary = "파일 단건 조회")
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<FileDto.UploadResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(fileService.findFileId(id)));
  }

  // 첨부 위치별 파일 목록 조회
  @Operation(summary = "첨부 위치별 파일 목록 조회")
  @GetMapping
  public ResponseEntity<ApiResponse<List<FileDto.SaveRequest>>> findByRef(
          @RequestParam("refType") String refType,
          @RequestParam("refId") Long refId) {
    return ResponseEntity.ok(ApiResponse.ok(fileService.findByRef(refType, refId)));
  }

  // 파일 DB 삭제
  @Operation(summary = "첨부 위치별 파일 삭제")
  @DeleteMapping("/delete")
  public ResponseEntity<ApiResponse<Void>> deleteFile(@RequestParam("refType") String refType, @RequestParam("refId") Long refId) {
    fileService.deleteFile(refType, refId);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }
}
