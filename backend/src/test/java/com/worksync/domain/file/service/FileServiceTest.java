// FileService 단위 테스트 (Supabase Storage 호출은 전부 Mock 처리)
package com.worksync.domain.file.service;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.file.dto.FileDto;
import com.worksync.domain.file.entity.FileAttachment;
import com.worksync.domain.file.entity.RefType;
import com.worksync.domain.file.repository.FileAttachmentRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** FileService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    private static final String SUPABASE_URL = "https://test.supabase.co";
    private static final String BUCKET = "WorkSync";

    @Mock
    private FileAttachmentRepository fileAttachmentRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private FileService fileService;

    @BeforeEach
    void setUp() {
        // RestClient는 필드 초기화(`= RestClient.create()`)로 생성되어 생성자 주입 대상이 아니므로
        // 리플렉션으로 교체한다. post()/method(DELETE) 모두 RequestBodyUriSpec을 반환하므로
        // 체이닝 전체를 하나의 mock으로 재현해 실제 네트워크 호출을 차단한다.
        ReflectionTestUtils.setField(fileService, "restClient", restClient);
        ReflectionTestUtils.setField(fileService, "supabaseUrl", SUPABASE_URL);
        ReflectionTestUtils.setField(fileService, "serviceRoleKey", "test-service-role-key");

        lenient().when(restClient.post()).thenReturn(requestSpec);
        lenient().when(restClient.method(HttpMethod.DELETE)).thenReturn(requestSpec);
        lenient().when(requestSpec.uri(anyString())).thenReturn(requestSpec);
        lenient().when(requestSpec.header(anyString(), anyString())).thenReturn(requestSpec);
        lenient().when(requestSpec.contentType(any())).thenReturn(requestSpec);
        // body(Object)와 body(StreamingHttpOutputMessage.Body) 오버로드가 공존하므로
        // any(Object.class)로 타입을 명시해 body(Object) 오버로드에만 매칭시킨다.
        lenient().when(requestSpec.body(any(Object.class))).thenReturn(requestSpec);
        lenient().when(requestSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());
    }

    private Employee buildEmployee(Long id, String name) {
        return Employee.builder()
                .id(id)
                .empNo("EMP" + id)
                .name(name)
                .email(name + id + "@worksync.com")
                .password("encoded-password")
                .build();
    }

    @DisplayName("파일을 업로드하면 Supabase Storage에 저장되고 공개 URL을 반환한다")
    @Test
    void upload_success_returnsUploadResponseWithPublicUrl() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "dummy content".getBytes());

        // when
        FileDto.UploadResponse result = fileService.upload(file);

        // then
        assertThat(result.getOriginalName()).isEqualTo("report.pdf");
        assertThat(result.getFileSize()).isEqualTo(file.getSize());
        assertThat(result.getMimeType()).isEqualTo("application/pdf");
        assertThat(result.getFilePath())
                .startsWith(SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/");
    }

    @DisplayName("파일 바이트를 읽는 중 오류가 발생하면 예외가 발생한다")
    @Test
    void upload_ioExceptionReadingBytes_throwsFileUploadFailed() throws IOException {
        // given
        MultipartFile file = Mockito.mock(MultipartFile.class);
        given(file.getOriginalFilename()).willReturn("broken.txt");
        given(file.getBytes()).willThrow(new IOException("read error"));

        // when & then
        assertThatThrownBy(() -> fileService.upload(file))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);
    }

    @DisplayName("Supabase 업로드 중 예외가 발생하면 CustomException으로 전파된다")
    @Test
    void upload_supabaseUploadFails_throwsFileUploadFailed() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "dummy content".getBytes());
        given(requestSpec.retrieve()).willThrow(new RuntimeException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> fileService.upload(file))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);
    }

    @DisplayName("첨부 위치 ID를 지정해 저장하면 파일 정보가 저장된다")
    @Test
    void updateRefId_success_nonChatRefType() {
        // given
        Employee uploader = buildEmployee(1L, "김철수");
        FileDto.SaveRequest request = FileDto.SaveRequest.builder()
                .originalName("report.pdf")
                .filePath("path/to/file.pdf")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .refType("APPROVAL")
                .refId(10L)
                .build();

        given(employeeRepository.findById(1L)).willReturn(Optional.of(uploader));
        given(fileAttachmentRepository.save(any(FileAttachment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        FileDto.UploadResponse result = fileService.updateRefId(1L, request);

        // then
        assertThat(result.getOriginalName()).isEqualTo("report.pdf");
        verifyNoInteractions(messagingTemplate);
    }

    @DisplayName("첨부 위치가 채팅이면 저장 후 실시간 메시지를 전송한다")
    @Test
    void updateRefId_chatRefType_sendsWebSocketMessage() {
        // given
        Employee uploader = buildEmployee(1L, "김철수");
        FileDto.SaveRequest request = FileDto.SaveRequest.builder()
                .originalName("image.png")
                .filePath("path/to/image.png")
                .fileSize(2048L)
                .mimeType("image/png")
                .refType("CHAT")
                .refId(5L)
                .build();

        given(employeeRepository.findById(1L)).willReturn(Optional.of(uploader));
        given(fileAttachmentRepository.save(any(FileAttachment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        fileService.updateRefId(1L, request);

        // then
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/5/files"), any(FileDto.UploadResponse.class));
    }

    @DisplayName("업로드한 직원이 존재하지 않으면 예외가 발생한다")
    @Test
    void updateRefId_uploaderNotFound_throwsEmployeeNotFound() {
        // given
        FileDto.SaveRequest request = FileDto.SaveRequest.builder()
                .originalName("report.pdf").refType("APPROVAL").refId(10L).build();
        given(employeeRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(999L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @DisplayName("존재하지 않는 첨부 위치 유형이면 예외가 발생한다")
    @Test
    void updateRefId_invalidRefType_throwsIllegalArgumentException() {
        // given
        Employee uploader = buildEmployee(1L, "김철수");
        FileDto.SaveRequest request = FileDto.SaveRequest.builder()
                .originalName("report.pdf").refType("INVALID_TYPE").refId(10L).build();
        given(employeeRepository.findById(1L)).willReturn(Optional.of(uploader));

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("파일 ID로 조회하면 정상적으로 응답을 반환한다")
    @Test
    void findFileId_success() {
        // given
        Employee uploader = buildEmployee(1L, "김철수");
        FileAttachment fileAttachment = FileAttachment.builder()
                .id(1L).uploader(uploader).originalName("report.pdf")
                .filePath("path/to/file.pdf").fileSize(1024L).mimeType("application/pdf")
                .refType(RefType.APPROVAL).refId(10L).build();
        given(fileAttachmentRepository.findById(1L)).willReturn(Optional.of(fileAttachment));

        // when
        FileDto.UploadResponse result = fileService.findFileId(1L);

        // then
        assertThat(result.getOriginalName()).isEqualTo("report.pdf");
    }

    @DisplayName("존재하지 않는 파일 ID로 조회하면 예외가 발생한다")
    @Test
    void findFileId_notFound_throwsFileNotFound() {
        // given
        given(fileAttachmentRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.findFileId(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @DisplayName("첨부 위치로 파일 목록을 조회하면 매핑된 목록을 반환한다")
    @Test
    void findByRef_success_returnsMappedList() {
        // given
        Employee uploader = buildEmployee(1L, "김철수");
        FileAttachment fileAttachment = FileAttachment.builder()
                .id(1L).uploader(uploader).originalName("report.pdf")
                .filePath("path/to/file.pdf").fileSize(1024L).mimeType("application/pdf")
                .refType(RefType.APPROVAL).refId(10L).build();
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.APPROVAL, 10L))
                .willReturn(List.of(fileAttachment));

        // when
        List<FileDto.SaveRequest> result = fileService.findByRef("APPROVAL", 10L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOriginalName()).isEqualTo("report.pdf");
    }

    @DisplayName("존재하지 않는 첨부 위치 유형으로 조회하면 예외가 발생한다")
    @Test
    void findByRef_invalidRefType_throwsIllegalArgumentException() {
        // when & then
        assertThatThrownBy(() -> fileService.findByRef("INVALID_TYPE", 10L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("첨부 파일을 삭제하면 스토리지와 DB에서 모두 제거된다")
    @Test
    void deleteFile_success_deletesStorageAndDbRecords() {
        // given
        Employee uploader = buildEmployee(1L, "김철수");
        FileAttachment fileAttachment = FileAttachment.builder()
                .id(1L).uploader(uploader).originalName("report.pdf")
                .filePath(SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/uuid.pdf")
                .fileSize(1024L).mimeType("application/pdf")
                .refType(RefType.APPROVAL).refId(10L).build();
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.APPROVAL, 10L))
                .willReturn(List.of(fileAttachment));

        // when
        fileService.deleteFile("APPROVAL", 10L);

        // then
        verify(fileAttachmentRepository).delete(fileAttachment);
    }

    @DisplayName("스토리지 삭제가 실패해도 DB 레코드는 삭제된다")
    @Test
    void deleteFile_storageDeleteFails_stillDeletesDbRecord() {
        // given
        Employee uploader = buildEmployee(1L, "김철수");
        FileAttachment fileAttachment = FileAttachment.builder()
                .id(1L).uploader(uploader).originalName("report.pdf")
                .filePath(SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/uuid.pdf")
                .fileSize(1024L).mimeType("application/pdf")
                .refType(RefType.APPROVAL).refId(10L).build();
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.APPROVAL, 10L))
                .willReturn(List.of(fileAttachment));
        given(requestSpec.retrieve()).willThrow(new RuntimeException("Storage delete failed"));

        // when
        fileService.deleteFile("APPROVAL", 10L);

        // then
        verify(fileAttachmentRepository).delete(fileAttachment);
    }

    @DisplayName("첨부된 파일이 없으면 아무 것도 삭제하지 않는다")
    @Test
    void deleteFile_noAttachedFiles_noop() {
        // given
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.APPROVAL, 10L))
                .willReturn(List.of());

        // when
        fileService.deleteFile("APPROVAL", 10L);

        // then
        verify(fileAttachmentRepository, never()).delete(any());
    }

    @DisplayName("존재하지 않는 첨부 위치 유형으로 삭제를 시도하면 예외가 발생한다")
    @Test
    void deleteFile_invalidRefType_throwsIllegalArgumentException() {
        // when & then
        assertThatThrownBy(() -> fileService.deleteFile("INVALID_TYPE", 10L))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(fileAttachmentRepository);
    }
}
