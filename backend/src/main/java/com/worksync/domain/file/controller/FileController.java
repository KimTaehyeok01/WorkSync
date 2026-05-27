package com.worksync.domain.file.controller;

import com.worksync.domain.file.dto.FileUploadResponse;
import com.worksync.domain.file.service.FileService;
import com.worksync.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

  private final FileService fileService;

  // 파일 업로드
  @PostMapping("/upload")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<FileUploadResponse> upload(
          @RequestParam("file")MultipartFile file,
          @RequestParam("uploaderId") Long uploaderId,
          @RequestParam("refType") String refType,
          @RequestParam("refId") Long refId
          ) throws IOException{
    return ApiResponse.created(fileService.upload(file,uploaderId,refType,refId));
  }

  // 파일 단건 조회
  @GetMapping("/{id}")
  public ApiResponse<FileUploadResponse> findById(@PathVariable Long id){
    return ApiResponse.ok(fileService.findFileId(id));
  }

  //첨부 위치별 파일 목록 조회
  @GetMapping
  public ApiResponse<List<FileUploadResponse>> findByRef(
          @RequestParam String refType,
          @RequestParam Long refId
  ) {
    return ApiResponse.ok(fileService.findByRef(refType, refId));
  }

  // 파일 삭제
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    fileService.delete(id);
    return ApiResponse.ok(null);
  }
}
