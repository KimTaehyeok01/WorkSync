import { useState } from "react";
import { uploadFile, deleteFile } from "../domains/file/services/FileApi";

export default function useFileUpload(accessToken, refType) {
  const [files, setFiles] = useState([]); //파일 목록
  const [uploadedFile, setUploadedFile] = useState(null); //DB에 저장할 URL 목록
  const [isDragging, setIsDragging] = useState(false); //드래그 상태

  // 파일 추가
  const addFiles = async (newFiles) => {
    // 화면에 파일 추가
    setFiles((prev) => [...prev, ...newFiles.map((file) => ({ file }))]);

    // 스토리지에 업로드
    const fileData = new FormData();
    newFiles.forEach((file) => fileData.append("file", file));
    const result = await uploadFile(accessToken, fileData, refType);

    // filePath, originalName, fileSize, mimeType 데이터 저장
    if (result?.data) {
      setUploadedFile(result.data);
    }
  };

  // 파일 삭제
  const removeFiles = async (index) => {
    // 스토리지 + DB 삭제(백엔드에서 처리)
    if (uploadFile?.filePath) {
      await deleteFile(accessToken, uploadFile.filePath);
    }

    // 화면에 파일 제거
    setFiles((prev) => prev.filter((_, i) => i !== index));
    // filePath, originalName, fileSize, mimeType 데이터 제거
    setUploadedFile(null);
  };

  // 최종 저장 후 초기화
  const clearFiles = () => {
    setFiles([]);
    setUploadedFile(null);
  };

  return {
    files,
    setFiles,
    isDragging,
    setIsDragging,
    uploadedFile,
    addFiles,
    removeFiles,
    clearFiles,
  };
}
