import { useState } from "react";
import { updateFile } from "../domains/file/services/fileApi";
import useAuthContext from "../../../store/AuthContext";

export default function useFileUpload(accessToken, refType) {
  const [files, setFiles] = useState([]); //파일 목록
  const [uploadUrls, setUploadUrls] = useState([]); //DB에 저장할 URL 목록
  const [isDragging, setIsDragging] = useState(false); //드래그 상태

  // 파일 추가
  const addFiles = async (newFiles) => {
    // 화면에 파일 추가
    setFiles((prev) => [...prev, ...newFiles.map((file) => ({ file }))]);

    // 스토리지에 업로드
    const fileData = new FormData();
    newFiles.forEach((file) => fileData.append("file", file));
    const result = await uploadFile(accessToken, fileData, refType);

    // 스토리지에 업로드가 있으면, URL sessionStorage 임시 저장
    if (result?.filePath) {
      const saved = JSON.parse(sessionStorage.getItem("uploadUrls") || []);
      const updated = [...saved, result.filePath];
      sessionStorage.setItem("uploadUrls", JSON.stringify(updated));
      setUploadUrls(updated);
    }
  };

  // 파일 삭제
  const removeFiles = (index) => {
    // 화면에 파일 제거
    setFiles((prev) => prev.filter((_, i) => i !== index));

    // 스토리지, URL sessionStorage 목록 제거
    const updated = uploadUrls.filter((_, i) => i !== index);
    sessionStorage.setItem("uploadUrls", JSON.stringify(updated));
    setUploadUrls(updated);
  };

  // 최종 저장 후 초기화
  const clearFiles = () => {
    setFiles([]);
    setUploadUrls([]);
    sessionStorage.removeItem("uploadUrls");
  };

  return {
    files,
    uploadUrls,
    isDragging,
    setIsDragging,
    addFiles,
    removeFiles,
    clearFiles,
  };
}
