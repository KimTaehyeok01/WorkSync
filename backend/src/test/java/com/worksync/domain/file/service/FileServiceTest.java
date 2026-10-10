// FileService 단위 테스트 (Supabase Storage 호출은 전부 Mock 처리)
package com.worksync.domain.file.service;

import com.worksync.domain.approval.entity.ApprovalDoc;
import com.worksync.domain.approval.entity.ApprovalLine;
import com.worksync.domain.approval.entity.StepType;
import com.worksync.domain.approval.repository.ApprovalDocRepository;
import com.worksync.domain.board.entity.Board;
import com.worksync.domain.board.entity.BoardType;
import com.worksync.domain.board.entity.Post;
import com.worksync.domain.board.repository.PostRepository;
import com.worksync.domain.chat.repository.ChatMemberRepository;
import com.worksync.domain.department.entity.Department;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.file.dto.FileDto;
import com.worksync.domain.file.entity.FileAttachment;
import com.worksync.domain.file.entity.RefType;
import com.worksync.domain.file.repository.FileAttachmentRepository;
import com.worksync.domain.mail.entity.Mail;
import com.worksync.domain.mail.entity.MailRecipient;
import com.worksync.domain.mail.entity.RecipientType;
import com.worksync.domain.mail.repository.MailRecipientRepository;
import com.worksync.domain.mail.repository.MailRepository;
import com.worksync.domain.task.entity.Task;
import com.worksync.domain.task.repository.TaskRepository;
import com.worksync.domain.task.service.TaskAccessPolicy;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import com.worksync.global.security.CurrentEmployeeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
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
import static org.mockito.ArgumentMatchers.anyCollection;
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
    private TaskRepository taskRepository;
    @Mock
    private ApprovalDocRepository approvalDocRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private ChatMemberRepository chatMemberRepository;
    @Mock
    private MailRepository mailRepository;
    @Mock
    private MailRecipientRepository mailRecipientRepository;
    @Mock
    private CurrentEmployeeProvider currentEmployeeProvider;
    // 업무 첨부 권한은 TaskService 와 같은 실제 규칙으로 판정한다
    @Spy
    private TaskAccessPolicy taskAccessPolicy = new TaskAccessPolicy();
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

    private Employee buildEmployee(Long id, String name, EmployeeRole role, Department department) {
        return Employee.builder()
                .id(id)
                .empNo("EMP" + id)
                .name(name)
                .email(name + id + "@worksync.com")
                .password("encoded-password")
                .role(role)
                .department(department)
                .build();
    }

    private Department buildDepartment(Long id, String name) {
        return Department.builder().id(id).name(name).build();
    }

    // 조회·삭제는 CurrentEmployeeProvider 로 로그인한 직원을 얻는다 (추가는 uploaderId 로 조회한 직원 기준)
    private void loginAs(Employee employee) {
        given(currentEmployeeProvider.getCurrentEmployee()).willReturn(employee);
    }

    private Task buildTask(Long id, Employee creator, Employee assignee, Department department) {
        return Task.builder()
                .id(id)
                .creator(creator)
                .assignee(assignee)
                .department(department)
                .title("주간 보고서 작성")
                .build();
    }

    private ApprovalDoc buildApprovalDoc(Long id, Employee drafter, ApprovalLine... lines) {
        return ApprovalDoc.builder()
                .id(id)
                .drafter(drafter)
                .title("지출 결의서")
                .approvalLines(List.of(lines))
                .build();
    }

    private ApprovalLine buildApprovalLine(Employee approver, StepType stepType) {
        return ApprovalLine.builder().approver(approver).stepType(stepType).stepOrder(1).build();
    }

    private Board buildBoard(Long id, BoardType boardType, Department department) {
        return Board.builder().id(id).boardType(boardType).name("게시판").department(department).build();
    }

    private Post buildPost(Long id, Board board, Employee author) {
        return Post.builder().id(id).board(board).author(author).title("제목").content("내용").build();
    }

    // 수신자(TO)가 한 명 있는 메일
    private Mail buildMail(Long id, Employee sender, Employee recipient) {
        Mail mail = Mail.builder().id(id).sender(sender).subject("주간 회의 자료").content("내용").build();
        mail.addRecipient(recipient, RecipientType.TO);
        return mail;
    }

    private FileAttachment buildFile(RefType refType, Long refId) {
        return FileAttachment.builder()
                .id(1L).uploader(buildEmployee(1L, "김철수")).originalName("report.pdf")
                .filePath(SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/uuid.pdf")
                .fileSize(1024L).mimeType("application/pdf")
                .refType(refType).refId(refId).build();
    }

    // 업로드 API 가 돌려주는 공개 URL 형식
    private String publicUrl(String objectPath) {
        return SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/" + objectPath;
    }

    private FileDto.SaveRequest buildSaveRequest(String refType, Long refId) {
        return buildSaveRequest(refType, refId, publicUrl("uuid.pdf"));
    }

    private FileDto.SaveRequest buildSaveRequest(String refType, Long refId, String filePath) {
        return FileDto.SaveRequest.builder()
                .originalName("report.pdf")
                .filePath(filePath)
                .fileSize(1024L)
                .mimeType("application/pdf")
                .refType(refType)
                .refId(refId)
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
        // given — 기안자 본인이 자신의 결재 문서에 첨부한다
        Employee uploader = buildEmployee(1L, "김철수");
        FileDto.SaveRequest request = FileDto.SaveRequest.builder()
                .originalName("report.pdf")
                .filePath(publicUrl("uuid.pdf"))
                .fileSize(1024L)
                .mimeType("application/pdf")
                .refType("APPROVAL")
                .refId(10L)
                .build();

        given(employeeRepository.findById(1L)).willReturn(Optional.of(uploader));
        given(approvalDocRepository.findById(10L)).willReturn(Optional.of(buildApprovalDoc(10L, uploader)));
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
        // given — 채팅방 참여자가 첨부한다
        Employee uploader = buildEmployee(1L, "김철수");
        FileDto.SaveRequest request = FileDto.SaveRequest.builder()
                .originalName("image.png")
                .filePath(publicUrl("uuid.png"))
                .fileSize(2048L)
                .mimeType("image/png")
                .refType("CHAT")
                .refId(5L)
                .build();

        given(employeeRepository.findById(1L)).willReturn(Optional.of(uploader));
        given(chatMemberRepository.existsByRoomIdAndEmployeeId(5L, 1L)).willReturn(true);
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
        // given — 기안자가 자신의 결재 문서 첨부를 조회한다
        Employee uploader = buildEmployee(1L, "김철수");
        FileAttachment fileAttachment = FileAttachment.builder()
                .id(1L).uploader(uploader).originalName("report.pdf")
                .filePath("path/to/file.pdf").fileSize(1024L).mimeType("application/pdf")
                .refType(RefType.APPROVAL).refId(10L).build();
        loginAs(uploader);
        given(fileAttachmentRepository.findById(1L)).willReturn(Optional.of(fileAttachment));
        given(approvalDocRepository.findWithDetailsById(10L))
                .willReturn(Optional.of(buildApprovalDoc(10L, uploader)));

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

    @DisplayName("원글을 볼 수 없는 파일을 ID로 조회하면 존재하지 않는 파일로 응답한다")
    @Test
    void findFileId_notViewable_throwsFileNotFound() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, buildDepartment(1L, "개발팀"));
        loginAs(buildEmployee(3L, "이민수", EmployeeRole.USER, buildDepartment(2L, "영업팀")));
        given(fileAttachmentRepository.findById(1L)).willReturn(Optional.of(buildFile(RefType.TASK, 10L)));
        given(taskRepository.findById(10L)).willReturn(Optional.of(task));

        // when & then
        assertThatThrownBy(() -> fileService.findFileId(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @DisplayName("첨부 위치로 파일 목록을 조회하면 매핑된 목록을 반환한다")
    @Test
    void findByRef_success_returnsMappedList() {
        // given — 기안자가 자신의 결재 문서 첨부 목록을 조회한다
        Employee uploader = buildEmployee(1L, "김철수");
        FileAttachment fileAttachment = FileAttachment.builder()
                .id(1L).uploader(uploader).originalName("report.pdf")
                .filePath("path/to/file.pdf").fileSize(1024L).mimeType("application/pdf")
                .refType(RefType.APPROVAL).refId(10L).build();
        loginAs(uploader);
        given(approvalDocRepository.findWithDetailsById(10L))
                .willReturn(Optional.of(buildApprovalDoc(10L, uploader)));
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
        // given — 기안자가 자신의 결재 문서 첨부를 삭제한다
        Employee uploader = buildEmployee(1L, "김철수");
        FileAttachment fileAttachment = FileAttachment.builder()
                .id(1L).uploader(uploader).originalName("report.pdf")
                .filePath(SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/uuid.pdf")
                .fileSize(1024L).mimeType("application/pdf")
                .refType(RefType.APPROVAL).refId(10L).build();
        loginAs(uploader);
        given(approvalDocRepository.findById(10L)).willReturn(Optional.of(buildApprovalDoc(10L, uploader)));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.APPROVAL, 10L))
                .willReturn(List.of(fileAttachment));

        // when
        fileService.deleteFile("APPROVAL", 10L);

        // then — 다른 첨부가 같은 파일을 쓰지 않으니 스토리지 객체도 지운다
        verify(fileAttachmentRepository).delete(fileAttachment);
        verify(restClient).method(HttpMethod.DELETE);
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
        loginAs(uploader);
        given(approvalDocRepository.findById(10L)).willReturn(Optional.of(buildApprovalDoc(10L, uploader)));
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
        Employee drafter = buildEmployee(1L, "김철수");
        loginAs(drafter);
        given(approvalDocRepository.findById(10L)).willReturn(Optional.of(buildApprovalDoc(10L, drafter)));
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

    // === 원글 없음 — 권한 없음과 똑같이 응답해 원글 존재 여부를 숨긴다 ===

    @DisplayName("원글이 없는 첨부 목록은 관리자가 조회해도 권한 없음으로 응답한다")
    @Test
    void findByRef_sourceNotFound_throwsForbidden() {
        // given
        loginAs(buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null));
        given(taskRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.findByRef("TASK", 999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("원글이 없는 위치에 첨부를 추가하면 권한 없음으로 응답한다")
    @Test
    void updateRefId_sourceNotFound_throwsForbidden() {
        // given
        Employee uploader = buildEmployee(1L, "김철수");
        given(employeeRepository.findById(1L)).willReturn(Optional.of(uploader));
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(1L, buildSaveRequest("POST", 999L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("원글이 없는 위치의 첨부를 삭제하면 권한 없음으로 응답한다")
    @Test
    void deleteFile_sourceNotFound_throwsForbidden() {
        // given
        loginAs(buildEmployee(1L, "김철수"));
        given(approvalDocRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.deleteFile("APPROVAL", 999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    // === TASK 첨부 권한 ===

    @DisplayName("업무와 무관한 직원은 업무 첨부 목록을 조회할 수 없다")
    @Test
    void findByRef_task_unrelatedEmployee_throwsForbidden() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(10L, creator, assignee, buildDepartment(1L, "개발팀"));
        loginAs(buildEmployee(3L, "이민수", EmployeeRole.USER, buildDepartment(2L, "영업팀")));
        given(taskRepository.findById(10L)).willReturn(Optional.of(task));

        // when & then
        assertThatThrownBy(() -> fileService.findByRef("TASK", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("업무 담당자는 업무 첨부 목록을 조회할 수 있다")
    @Test
    void findByRef_task_assignee_success() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(10L, creator, assignee, null);
        loginAs(assignee);
        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.TASK, 10L))
                .willReturn(List.of(buildFile(RefType.TASK, 10L)));

        // when
        List<FileDto.SaveRequest> result = fileService.findByRef("TASK", 10L);

        // then
        assertThat(result).hasSize(1);
    }

    @DisplayName("업무 담당자는 업무에 첨부를 추가할 수 없다")
    @Test
    void updateRefId_task_assignee_throwsForbidden() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(10L, creator, assignee, null);
        given(employeeRepository.findById(2L)).willReturn(Optional.of(assignee));
        given(taskRepository.findById(10L)).willReturn(Optional.of(task));

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(2L, buildSaveRequest("TASK", 10L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("업무 작성자는 업무 첨부를 삭제할 수 있다")
    @Test
    void deleteFile_task_creator_success() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, null);
        FileAttachment file = buildFile(RefType.TASK, 10L);
        loginAs(creator);
        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.TASK, 10L)).willReturn(List.of(file));

        // when
        fileService.deleteFile("TASK", 10L);

        // then
        verify(fileAttachmentRepository).delete(file);
    }

    @DisplayName("관리자라도 업무 작성자가 아니면 업무 첨부를 삭제할 수 없다")
    @Test
    void deleteFile_task_adminNotCreator_throwsForbidden() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, null);
        loginAs(buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null));
        given(taskRepository.findById(10L)).willReturn(Optional.of(task));

        // when & then
        assertThatThrownBy(() -> fileService.deleteFile("TASK", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository, restClient);
    }

    // === APPROVAL 첨부 권한 ===

    @DisplayName("결재선의 참조인은 결재 문서 첨부 목록을 조회할 수 있다")
    @Test
    void findByRef_approval_referenceOnLine_success() {
        // given
        Employee drafter = buildEmployee(1L, "김철수");
        Employee reference = buildEmployee(4L, "최참조");
        ApprovalDoc doc = buildApprovalDoc(10L, drafter,
                buildApprovalLine(buildEmployee(2L, "박영희"), StepType.APPROVE),
                buildApprovalLine(reference, StepType.REFERENCE));
        loginAs(reference);
        given(approvalDocRepository.findWithDetailsById(10L)).willReturn(Optional.of(doc));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.APPROVAL, 10L))
                .willReturn(List.of(buildFile(RefType.APPROVAL, 10L)));

        // when
        List<FileDto.SaveRequest> result = fileService.findByRef("APPROVAL", 10L);

        // then
        assertThat(result).hasSize(1);
    }

    @DisplayName("기안자도 결재선에도 없는 직원은 관리자라도 결재 문서 첨부 목록을 조회할 수 없다")
    @Test
    void findByRef_approval_unrelatedEmployee_throwsForbidden() {
        // given
        Employee drafter = buildEmployee(1L, "김철수");
        ApprovalDoc doc = buildApprovalDoc(10L, drafter,
                buildApprovalLine(buildEmployee(2L, "박영희"), StepType.APPROVE));
        loginAs(buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null));
        given(approvalDocRepository.findWithDetailsById(10L)).willReturn(Optional.of(doc));

        // when & then
        assertThatThrownBy(() -> fileService.findByRef("APPROVAL", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("결재자는 결재 문서 첨부를 삭제할 수 없다")
    @Test
    void deleteFile_approval_approver_throwsForbidden() {
        // given
        Employee drafter = buildEmployee(1L, "김철수");
        Employee approver = buildEmployee(2L, "박영희");
        ApprovalDoc doc = buildApprovalDoc(10L, drafter, buildApprovalLine(approver, StepType.APPROVE));
        loginAs(approver);
        given(approvalDocRepository.findById(10L)).willReturn(Optional.of(doc));

        // when & then
        assertThatThrownBy(() -> fileService.deleteFile("APPROVAL", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository, restClient);
    }

    // === POST 첨부 권한 ===

    @DisplayName("다른 부서의 부서게시판 글 첨부 목록은 조회할 수 없다")
    @Test
    void findByRef_post_otherDepartmentBoard_throwsForbidden() {
        // given
        Department department = buildDepartment(1L, "개발팀");
        Employee author = buildEmployee(1L, "김철수", EmployeeRole.USER, department);
        Post post = buildPost(10L, buildBoard(1L, BoardType.DEPARTMENT, department), author);
        loginAs(buildEmployee(3L, "이민수", EmployeeRole.USER, buildDepartment(2L, "영업팀")));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> fileService.findByRef("POST", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("같은 부서 직원은 부서게시판 글 첨부 목록을 조회할 수 있다")
    @Test
    void findByRef_post_sameDepartmentBoard_success() {
        // given
        Department department = buildDepartment(1L, "개발팀");
        Employee author = buildEmployee(1L, "김철수", EmployeeRole.USER, department);
        Post post = buildPost(10L, buildBoard(1L, BoardType.DEPARTMENT, department), author);
        loginAs(buildEmployee(3L, "이민수", EmployeeRole.USER, buildDepartment(1L, "개발팀")));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.POST, 10L))
                .willReturn(List.of(buildFile(RefType.POST, 10L)));

        // when
        List<FileDto.SaveRequest> result = fileService.findByRef("POST", 10L);

        // then
        assertThat(result).hasSize(1);
    }

    @DisplayName("관리자는 다른 부서의 부서게시판 글 첨부 목록도 조회할 수 있다")
    @Test
    void findByRef_post_departmentBoard_admin_success() {
        // given
        Department department = buildDepartment(1L, "개발팀");
        Employee author = buildEmployee(1L, "김철수", EmployeeRole.USER, department);
        Post post = buildPost(10L, buildBoard(1L, BoardType.DEPARTMENT, department), author);
        loginAs(buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.POST, 10L))
                .willReturn(List.of(buildFile(RefType.POST, 10L)));

        // when
        List<FileDto.SaveRequest> result = fileService.findByRef("POST", 10L);

        // then
        assertThat(result).hasSize(1);
    }

    @DisplayName("일반 게시판 글 첨부 목록은 누구나 조회할 수 있다")
    @Test
    void findByRef_post_generalBoard_anyone_success() {
        // given
        Employee author = buildEmployee(1L, "김철수");
        Post post = buildPost(10L, buildBoard(2L, BoardType.FREE, null), author);
        loginAs(buildEmployee(3L, "이민수", EmployeeRole.USER, null));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.POST, 10L))
                .willReturn(List.of(buildFile(RefType.POST, 10L)));

        // when
        List<FileDto.SaveRequest> result = fileService.findByRef("POST", 10L);

        // then
        assertThat(result).hasSize(1);
    }

    @DisplayName("관리자라도 글 작성자가 아니면 게시글 첨부를 삭제할 수 없다")
    @Test
    void deleteFile_post_adminNotAuthor_throwsForbidden() {
        // given
        Employee author = buildEmployee(1L, "김철수");
        Post post = buildPost(10L, buildBoard(2L, BoardType.FREE, null), author);
        loginAs(buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> fileService.deleteFile("POST", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository, restClient);
    }

    // === CHAT 첨부 권한 ===

    @DisplayName("채팅방 참여자가 아니면 채팅 첨부 목록을 조회할 수 없다")
    @Test
    void findByRef_chat_nonMember_throwsForbidden() {
        // given
        loginAs(buildEmployee(3L, "이민수"));
        given(chatMemberRepository.existsByRoomIdAndEmployeeId(5L, 3L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> fileService.findByRef("CHAT", 5L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("채팅방 참여자가 아니면 채팅방에 첨부를 추가할 수 없다")
    @Test
    void updateRefId_chat_nonMember_throwsForbidden() {
        // given
        Employee outsider = buildEmployee(3L, "이민수");
        given(employeeRepository.findById(3L)).willReturn(Optional.of(outsider));
        given(chatMemberRepository.existsByRoomIdAndEmployeeId(5L, 3L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(3L, buildSaveRequest("CHAT", 5L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository, messagingTemplate);
    }

    @DisplayName("채팅 첨부는 채팅방 참여자라도 삭제할 수 없다")
    @Test
    void deleteFile_chat_member_throwsForbidden() {
        // given — 참여 여부와 무관하게 거부되므로 참여자 조회조차 하지 않는다
        loginAs(buildEmployee(1L, "김철수"));

        // when & then
        assertThatThrownBy(() -> fileService.deleteFile("CHAT", 5L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(chatMemberRepository, fileAttachmentRepository, restClient);
    }

    // === EMPLOYEE 첨부 권한 ===

    @DisplayName("직원 프로필 첨부 목록은 일반 직원도 조회할 수 있다")
    @Test
    void findByRef_employee_anyone_success() {
        // given
        loginAs(buildEmployee(3L, "이민수"));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.EMPLOYEE, 7L))
                .willReturn(List.of(buildFile(RefType.EMPLOYEE, 7L)));

        // when
        List<FileDto.SaveRequest> result = fileService.findByRef("EMPLOYEE", 7L);

        // then
        assertThat(result).hasSize(1);
    }

    @DisplayName("일반 직원은 직원 프로필에 첨부를 추가할 수 없다")
    @Test
    void updateRefId_employee_nonAdmin_throwsForbidden() {
        // given
        Employee uploader = buildEmployee(3L, "이민수");
        given(employeeRepository.findById(3L)).willReturn(Optional.of(uploader));

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(3L, buildSaveRequest("EMPLOYEE", 3L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("관리자는 직원 프로필에 첨부를 추가할 수 있다")
    @Test
    void updateRefId_employee_admin_success() {
        // given
        Employee admin = buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null);
        given(employeeRepository.findById(9L)).willReturn(Optional.of(admin));
        given(fileAttachmentRepository.save(any(FileAttachment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        FileDto.UploadResponse result = fileService.updateRefId(9L, buildSaveRequest("EMPLOYEE", 7L));

        // then
        assertThat(result.getOriginalName()).isEqualTo("report.pdf");
    }

    // === BOARD 첨부 권한 ===

    @DisplayName("일반 직원은 게시판 첨부 목록을 조회할 수 없다")
    @Test
    void findByRef_board_nonAdmin_throwsForbidden() {
        // given
        loginAs(buildEmployee(3L, "이민수"));

        // when & then
        assertThatThrownBy(() -> fileService.findByRef("BOARD", 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("관리자는 게시판 첨부 목록을 조회할 수 있다")
    @Test
    void findByRef_board_admin_success() {
        // given
        loginAs(buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.BOARD, 1L))
                .willReturn(List.of(buildFile(RefType.BOARD, 1L)));

        // when
        List<FileDto.SaveRequest> result = fileService.findByRef("BOARD", 1L);

        // then
        assertThat(result).hasSize(1);
    }

    @DisplayName("일반 직원은 게시판 첨부를 삭제할 수 없다")
    @Test
    void deleteFile_board_nonAdmin_throwsForbidden() {
        // given
        loginAs(buildEmployee(3L, "이민수"));

        // when & then
        assertThatThrownBy(() -> fileService.deleteFile("BOARD", 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository, restClient);
    }

    // === 결재 문서 첨부 추가 — 기안자만 ===

    @DisplayName("결재선의 결재자는 결재 문서에 첨부를 추가할 수 없다")
    @Test
    void updateRefId_approval_approver_throwsForbidden() {
        // given
        Employee drafter = buildEmployee(1L, "김철수");
        Employee approver = buildEmployee(2L, "박영희");
        ApprovalDoc doc = buildApprovalDoc(10L, drafter, buildApprovalLine(approver, StepType.APPROVE));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(approver));
        given(approvalDocRepository.findById(10L)).willReturn(Optional.of(doc));

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(2L, buildSaveRequest("APPROVAL", 10L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("관리자라도 기안자가 아니면 결재 문서에 첨부를 추가할 수 없다")
    @Test
    void updateRefId_approval_admin_throwsForbidden() {
        // given
        Employee drafter = buildEmployee(1L, "김철수");
        Employee admin = buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null);
        given(employeeRepository.findById(9L)).willReturn(Optional.of(admin));
        given(approvalDocRepository.findById(10L)).willReturn(Optional.of(buildApprovalDoc(10L, drafter)));

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(9L, buildSaveRequest("APPROVAL", 10L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    // === 소문자 refType — 변환 뒤 같은 인가·같은 푸시를 탄다 ===

    @DisplayName("소문자 refType 으로 조회해도 같은 인가가 적용된다")
    @Test
    void findByRef_lowercaseRefType_appliesSameAuthorization() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, buildDepartment(1L, "개발팀"));
        loginAs(buildEmployee(3L, "이민수", EmployeeRole.USER, buildDepartment(2L, "영업팀")));
        given(taskRepository.findById(10L)).willReturn(Optional.of(task));

        // when & then
        assertThatThrownBy(() -> fileService.findByRef("task", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("소문자 refType 으로 삭제해도 같은 인가가 적용되고 스토리지는 호출되지 않는다")
    @Test
    void deleteFile_lowercaseRefType_appliesSameAuthorization() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(10L, creator, assignee, null);
        loginAs(assignee);
        given(taskRepository.findById(10L)).willReturn(Optional.of(task));

        // when & then
        assertThatThrownBy(() -> fileService.deleteFile("task", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository, restClient);
    }

    @DisplayName("소문자 chat 으로 저장해도 채팅방 실시간 메시지가 전송된다")
    @Test
    void updateRefId_lowercaseChat_sendsWebSocketMessage() {
        // given
        Employee uploader = buildEmployee(1L, "김철수");
        given(employeeRepository.findById(1L)).willReturn(Optional.of(uploader));
        given(chatMemberRepository.existsByRoomIdAndEmployeeId(5L, 1L)).willReturn(true);
        given(fileAttachmentRepository.save(any(FileAttachment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        fileService.updateRefId(1L, buildSaveRequest("chat", 5L));

        // then
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/5/files"), any(FileDto.UploadResponse.class));
    }

    // === refId 없음 — 500 대신 권한 없음으로 응답 ===

    @DisplayName("refId 없이 첨부를 저장하면 권한 없음으로 응답한다")
    @Test
    void updateRefId_nullRefId_throwsForbidden() {
        // given
        Employee uploader = buildEmployee(1L, "김철수");
        given(employeeRepository.findById(1L)).willReturn(Optional.of(uploader));

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(1L, buildSaveRequest("TASK", null)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(taskRepository, fileAttachmentRepository);
    }

    @DisplayName("refId 없이 목록을 조회하면 전원 허용인 유형이어도 권한 없음으로 응답한다")
    @Test
    void findByRef_nullRefId_throwsForbidden() {
        // given
        loginAs(buildEmployee(3L, "이민수"));

        // when & then
        assertThatThrownBy(() -> fileService.findByRef("EMPLOYEE", null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("refId 없이 삭제하면 권한 없음으로 응답하고 스토리지는 호출되지 않는다")
    @Test
    void deleteFile_nullRefId_throwsForbidden() {
        // given
        loginAs(buildEmployee(1L, "김철수"));

        // when & then
        assertThatThrownBy(() -> fileService.deleteFile("TASK", null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(taskRepository, fileAttachmentRepository, restClient);
    }

    // === filePath 검증 — 이 서비스가 업로드한 파일의 공개 URL 만 저장한다 ===

    @DisplayName("공개 URL 접두사로 시작하지 않는 filePath 는 저장할 수 없다")
    @Test
    void updateRefId_filePathWithoutPublicPrefix_throwsInvalidRequest() {
        // given — 기안자 본인이 자신의 결재 문서에 첨부하지만 외부 URL·상대 경로를 넣는다
        Employee drafter = buildEmployee(1L, "김철수");
        given(employeeRepository.findById(1L)).willReturn(Optional.of(drafter));
        given(approvalDocRepository.findById(10L)).willReturn(Optional.of(buildApprovalDoc(10L, drafter)));

        for (String badPath : new String[]{
                "https://evil.example.com/storage/v1/object/public/" + BUCKET + "/uuid.pdf",
                "path/to/file.pdf",
                SUPABASE_URL + "/storage/v1/object/public/OtherBucket/uuid.pdf",
                null}) {
            // when & then
            assertThatThrownBy(() -> fileService.updateRefId(1L, buildSaveRequest("APPROVAL", 10L, badPath)))
                    .as(String.valueOf(badPath))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("접두사 뒤에 경로 조작 문자가 있는 filePath 는 저장할 수 없다")
    @Test
    void updateRefId_filePathWithPathTraversal_throwsInvalidRequest() {
        // given
        Employee drafter = buildEmployee(1L, "김철수");
        given(employeeRepository.findById(1L)).willReturn(Optional.of(drafter));
        given(approvalDocRepository.findById(10L)).willReturn(Optional.of(buildApprovalDoc(10L, drafter)));

        for (String objectPath : new String[]{"../other-user.pdf", "sub/dir.pdf", "a\\b.pdf", "uuid..pdf", ""}) {
            // when & then
            assertThatThrownBy(() -> fileService.updateRefId(
                    1L, buildSaveRequest("APPROVAL", 10L, publicUrl(objectPath))))
                    .as(objectPath)
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }
        verifyNoInteractions(fileAttachmentRepository);
    }

    // === 삭제 — 같은 스토리지 파일을 다른 첨부가 쓰고 있으면 객체는 남긴다 ===

    // === MAIL 첨부 권한 — 조회는 보낸 사람·수신자, 추가·삭제는 보낸 사람만 ===

    @DisplayName("보낸 사람은 메일 첨부 목록을 조회할 수 있다")
    @Test
    void findByRef_mail_sender_success() {
        // given
        Employee sender = buildEmployee(1L, "김철수");
        loginAs(sender);
        given(mailRepository.findById(10L))
                .willReturn(Optional.of(buildMail(10L, sender, buildEmployee(2L, "박영희"))));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.MAIL, 10L))
                .willReturn(List.of(buildFile(RefType.MAIL, 10L)));

        // when
        List<FileDto.SaveRequest> result = fileService.findByRef("MAIL", 10L);

        // then
        assertThat(result).hasSize(1);
    }

    @DisplayName("받는 사람은 메일 첨부 목록을 조회할 수 있다")
    @Test
    void findByRef_mail_recipient_success() {
        // given
        Employee recipient = buildEmployee(2L, "박영희");
        Mail mail = buildMail(10L, buildEmployee(1L, "김철수"), recipient);
        loginAs(recipient);
        given(mailRepository.findById(10L)).willReturn(Optional.of(mail));
        given(mailRecipientRepository.findByMailIdAndRecipientId(10L, 2L))
                .willReturn(Optional.of(mail.getRecipients().get(0)));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.MAIL, 10L))
                .willReturn(List.of(buildFile(RefType.MAIL, 10L)));

        // when
        List<FileDto.SaveRequest> result = fileService.findByRef("MAIL", 10L);

        // then
        assertThat(result).hasSize(1);
    }

    @DisplayName("보낸 사람도 수신자도 아닌 직원은 메일 첨부 목록을 조회할 수 없다")
    @Test
    void findByRef_mail_stranger_throwsForbidden() {
        // given
        Mail mail = buildMail(10L, buildEmployee(1L, "김철수"), buildEmployee(2L, "박영희"));
        loginAs(buildEmployee(3L, "이민수"));
        given(mailRepository.findById(10L)).willReturn(Optional.of(mail));
        given(mailRecipientRepository.findByMailIdAndRecipientId(10L, 3L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.findByRef("MAIL", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("받는 사람이 자기 받은편지함에서 지운 메일의 첨부 목록은 조회할 수 없다")
    @Test
    void findByRef_mail_recipientDeleted_throwsForbidden() {
        // given
        Employee recipient = buildEmployee(2L, "박영희");
        Mail mail = buildMail(10L, buildEmployee(1L, "김철수"), recipient);
        MailRecipient mine = mail.getRecipients().get(0);
        mine.delete();
        loginAs(recipient);
        given(mailRepository.findById(10L)).willReturn(Optional.of(mail));
        given(mailRecipientRepository.findByMailIdAndRecipientId(10L, 2L)).willReturn(Optional.of(mine));

        // when & then
        assertThatThrownBy(() -> fileService.findByRef("MAIL", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("보낸 사람이 보낸편지함에서 지운 메일의 첨부 목록은 조회할 수 없다")
    @Test
    void findByRef_mail_senderDeleted_throwsForbidden() {
        // given
        Employee sender = buildEmployee(1L, "김철수");
        Mail mail = buildMail(10L, sender, buildEmployee(2L, "박영희"));
        mail.deleteBySender();
        loginAs(sender);
        given(mailRepository.findById(10L)).willReturn(Optional.of(mail));
        given(mailRecipientRepository.findByMailIdAndRecipientId(10L, 1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.findByRef("MAIL", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("보낸 편지함에서 지웠어도 자기에게도 보낸 메일이면 받은편지함 수신자로서 첨부 목록을 조회할 수 있다")
    @Test
    void findByRef_mail_senderDeletedButStillRecipient_success() {
        // given
        Employee sender = buildEmployee(1L, "김철수");
        Mail mail = buildMail(10L, sender, sender);
        mail.deleteBySender();
        loginAs(sender);
        given(mailRepository.findById(10L)).willReturn(Optional.of(mail));
        given(mailRecipientRepository.findByMailIdAndRecipientId(10L, 1L))
                .willReturn(Optional.of(mail.getRecipients().get(0)));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.MAIL, 10L))
                .willReturn(List.of(buildFile(RefType.MAIL, 10L)));

        // when
        List<FileDto.SaveRequest> result = fileService.findByRef("MAIL", 10L);

        // then
        assertThat(result).hasSize(1);
    }

    @DisplayName("보낸 사람은 메일에 첨부를 추가할 수 있다")
    @Test
    void updateRefId_mail_sender_success() {
        // given
        Employee sender = buildEmployee(1L, "김철수");
        given(employeeRepository.findById(1L)).willReturn(Optional.of(sender));
        given(mailRepository.findById(10L))
                .willReturn(Optional.of(buildMail(10L, sender, buildEmployee(2L, "박영희"))));
        given(fileAttachmentRepository.save(any(FileAttachment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        FileDto.UploadResponse result = fileService.updateRefId(1L, buildSaveRequest("MAIL", 10L));

        // then
        assertThat(result.getOriginalName()).isEqualTo("report.pdf");
        verifyNoInteractions(messagingTemplate);
    }

    @DisplayName("받는 사람은 조회할 수 있어도 메일에 첨부를 추가할 수 없다")
    @Test
    void updateRefId_mail_recipient_throwsForbidden() {
        // given
        Employee recipient = buildEmployee(2L, "박영희");
        given(employeeRepository.findById(2L)).willReturn(Optional.of(recipient));
        given(mailRepository.findById(10L))
                .willReturn(Optional.of(buildMail(10L, buildEmployee(1L, "김철수"), recipient)));

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(2L, buildSaveRequest("MAIL", 10L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository, mailRecipientRepository);
    }

    @DisplayName("무관한 직원은 메일에 첨부를 추가할 수 없다")
    @Test
    void updateRefId_mail_stranger_throwsForbidden() {
        // given
        Employee stranger = buildEmployee(3L, "이민수");
        given(employeeRepository.findById(3L)).willReturn(Optional.of(stranger));
        given(mailRepository.findById(10L))
                .willReturn(Optional.of(buildMail(10L, buildEmployee(1L, "김철수"), buildEmployee(2L, "박영희"))));

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(3L, buildSaveRequest("MAIL", 10L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("보낸 사람이 보낸편지함에서 지운 메일에는 첨부를 추가할 수 없다")
    @Test
    void updateRefId_mail_senderDeleted_throwsForbidden() {
        // given
        Employee sender = buildEmployee(1L, "김철수");
        Mail mail = buildMail(10L, sender, buildEmployee(2L, "박영희"));
        mail.deleteBySender();
        given(employeeRepository.findById(1L)).willReturn(Optional.of(sender));
        given(mailRepository.findById(10L)).willReturn(Optional.of(mail));

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(1L, buildSaveRequest("MAIL", 10L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("존재하지 않는 메일에 첨부를 추가하면 권한 없음으로 응답한다")
    @Test
    void updateRefId_mail_notFound_throwsForbidden() {
        // given
        Employee sender = buildEmployee(1L, "김철수");
        given(employeeRepository.findById(1L)).willReturn(Optional.of(sender));
        given(mailRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(1L, buildSaveRequest("MAIL", 999L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("보낸 사람은 메일 첨부를 삭제할 수 있고 스토리지 객체도 지운다")
    @Test
    void deleteFile_mail_sender_success() {
        // given
        Employee sender = buildEmployee(1L, "김철수");
        FileAttachment file = buildFile(RefType.MAIL, 10L);
        loginAs(sender);
        given(mailRepository.findById(10L))
                .willReturn(Optional.of(buildMail(10L, sender, buildEmployee(2L, "박영희"))));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.MAIL, 10L)).willReturn(List.of(file));

        // when
        fileService.deleteFile("MAIL", 10L);

        // then
        verify(fileAttachmentRepository).delete(file);
        verify(restClient).method(HttpMethod.DELETE);
    }

    @DisplayName("받는 사람은 메일 첨부를 삭제할 수 없고 스토리지는 호출되지 않는다")
    @Test
    void deleteFile_mail_recipient_throwsForbidden() {
        // given
        Employee recipient = buildEmployee(2L, "박영희");
        loginAs(recipient);
        given(mailRepository.findById(10L))
                .willReturn(Optional.of(buildMail(10L, buildEmployee(1L, "김철수"), recipient)));

        // when & then
        assertThatThrownBy(() -> fileService.deleteFile("MAIL", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository, restClient);
    }

    @DisplayName("무관한 직원은 메일 첨부를 삭제할 수 없고 스토리지는 호출되지 않는다")
    @Test
    void deleteFile_mail_stranger_throwsForbidden() {
        // given
        loginAs(buildEmployee(3L, "이민수"));
        given(mailRepository.findById(10L))
                .willReturn(Optional.of(buildMail(10L, buildEmployee(1L, "김철수"), buildEmployee(2L, "박영희"))));

        // when & then
        assertThatThrownBy(() -> fileService.deleteFile("MAIL", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository, restClient);
    }

    @DisplayName("보낸 사람이 보낸편지함에서 지운 메일의 첨부는 삭제할 수 없고 스토리지는 호출되지 않는다")
    @Test
    void deleteFile_mail_senderDeleted_throwsForbidden() {
        // given
        Employee sender = buildEmployee(1L, "김철수");
        Mail mail = buildMail(10L, sender, buildEmployee(2L, "박영희"));
        mail.deleteBySender();
        loginAs(sender);
        given(mailRepository.findById(10L)).willReturn(Optional.of(mail));

        // when & then
        assertThatThrownBy(() -> fileService.deleteFile("MAIL", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository, restClient);
    }

    @DisplayName("존재하지 않는 메일의 첨부 목록 조회·삭제는 권한 없음으로 응답한다")
    @Test
    void findByRefAndDelete_mail_notFound_throwsForbidden() {
        // given
        loginAs(buildEmployee(1L, "김철수"));
        given(mailRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.findByRef("MAIL", 999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> fileService.deleteFile("MAIL", 999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository, restClient);
    }

    @DisplayName("소문자 mail 로 조회해도 같은 인가가 적용된다")
    @Test
    void findByRef_lowercaseMail_appliesSameAuthorization() {
        // given
        Mail mail = buildMail(10L, buildEmployee(1L, "김철수"), buildEmployee(2L, "박영희"));
        loginAs(buildEmployee(3L, "이민수"));
        given(mailRepository.findById(10L)).willReturn(Optional.of(mail));
        given(mailRecipientRepository.findByMailIdAndRecipientId(10L, 3L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.findByRef("mail", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("소문자 mail 로 삭제해도 같은 인가가 적용되고 스토리지는 호출되지 않는다")
    @Test
    void deleteFile_lowercaseMail_appliesSameAuthorization() {
        // given
        Employee recipient = buildEmployee(2L, "박영희");
        loginAs(recipient);
        given(mailRepository.findById(10L))
                .willReturn(Optional.of(buildMail(10L, buildEmployee(1L, "김철수"), recipient)));

        // when & then
        assertThatThrownBy(() -> fileService.deleteFile("mail", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository, restClient);
    }

    @DisplayName("소문자 mail 로 저장해도 같은 인가가 적용된다")
    @Test
    void updateRefId_lowercaseMail_appliesSameAuthorization() {
        // given
        Employee recipient = buildEmployee(2L, "박영희");
        given(employeeRepository.findById(2L)).willReturn(Optional.of(recipient));
        given(mailRepository.findById(10L))
                .willReturn(Optional.of(buildMail(10L, buildEmployee(1L, "김철수"), recipient)));

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(2L, buildSaveRequest("mail", 10L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("무관한 직원이 메일 첨부를 파일 ID 로 조회하면 존재하지 않는 파일로 응답한다")
    @Test
    void findFileId_mail_stranger_throwsFileNotFound() {
        // given
        Mail mail = buildMail(10L, buildEmployee(1L, "김철수"), buildEmployee(2L, "박영희"));
        loginAs(buildEmployee(3L, "이민수"));
        given(fileAttachmentRepository.findById(1L)).willReturn(Optional.of(buildFile(RefType.MAIL, 10L)));
        given(mailRepository.findById(10L)).willReturn(Optional.of(mail));
        given(mailRecipientRepository.findByMailIdAndRecipientId(10L, 3L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.findFileId(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    // 관리자는 메일 첨부에 예외가 없다 — 보낸 사람·받는 사람이 아니면 조회·추가·삭제 모두 거부
    @DisplayName("보낸 사람도 받는 사람도 아닌 관리자는 메일 첨부 목록을 조회할 수 없다")
    @Test
    void findByRef_mail_admin_throwsForbidden() {
        // given
        Mail mail = buildMail(10L, buildEmployee(1L, "김철수"), buildEmployee(2L, "박영희"));
        loginAs(buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null));
        given(mailRepository.findById(10L)).willReturn(Optional.of(mail));
        given(mailRecipientRepository.findByMailIdAndRecipientId(10L, 9L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.findByRef("MAIL", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("보낸 사람도 받는 사람도 아닌 관리자는 메일 첨부를 파일 ID 로 조회하면 존재하지 않는 파일로 응답한다")
    @Test
    void findFileId_mail_admin_throwsFileNotFound() {
        // given
        Mail mail = buildMail(10L, buildEmployee(1L, "김철수"), buildEmployee(2L, "박영희"));
        loginAs(buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null));
        given(fileAttachmentRepository.findById(1L)).willReturn(Optional.of(buildFile(RefType.MAIL, 10L)));
        given(mailRepository.findById(10L)).willReturn(Optional.of(mail));
        given(mailRecipientRepository.findByMailIdAndRecipientId(10L, 9L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.findFileId(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @DisplayName("관리자는 메일에 첨부를 추가할 수 없다")
    @Test
    void updateRefId_mail_admin_throwsForbidden() {
        // given
        Employee admin = buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null);
        given(employeeRepository.findById(9L)).willReturn(Optional.of(admin));
        given(mailRepository.findById(10L))
                .willReturn(Optional.of(buildMail(10L, buildEmployee(1L, "김철수"), buildEmployee(2L, "박영희"))));

        // when & then
        assertThatThrownBy(() -> fileService.updateRefId(9L, buildSaveRequest("MAIL", 10L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("관리자는 메일 첨부를 삭제할 수 없고 스토리지는 호출되지 않는다")
    @Test
    void deleteFile_mail_admin_throwsForbidden() {
        // given
        loginAs(buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null));
        given(mailRepository.findById(10L))
                .willReturn(Optional.of(buildMail(10L, buildEmployee(1L, "김철수"), buildEmployee(2L, "박영희"))));

        // when & then
        assertThatThrownBy(() -> fileService.deleteFile("MAIL", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(fileAttachmentRepository, restClient);
    }

    @DisplayName("같은 파일을 가리키는 다른 첨부가 남아 있으면 스토리지 객체는 지우지 않고 DB 행만 삭제한다")
    @Test
    void deleteFile_sharedFilePath_skipsStorageDelete() {
        // given
        Employee drafter = buildEmployee(1L, "김철수");
        FileAttachment fileAttachment = buildFile(RefType.APPROVAL, 10L);
        loginAs(drafter);
        given(approvalDocRepository.findById(10L)).willReturn(Optional.of(buildApprovalDoc(10L, drafter)));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.APPROVAL, 10L))
                .willReturn(List.of(fileAttachment));
        given(fileAttachmentRepository.existsByFilePathAndIdNotIn(eq(fileAttachment.getFilePath()), anyCollection()))
                .willReturn(true);

        // when
        fileService.deleteFile("APPROVAL", 10L);

        // then
        verify(fileAttachmentRepository).delete(fileAttachment);
        verifyNoInteractions(restClient);
    }

    @DisplayName("삭제 대상 첨부 자신은 같은 파일 여부 확인에서 제외한다")
    @Test
    void deleteFile_checksSharedFilePathExcludingDeletingIds() {
        // given
        Employee drafter = buildEmployee(1L, "김철수");
        FileAttachment fileAttachment = buildFile(RefType.APPROVAL, 10L);
        loginAs(drafter);
        given(approvalDocRepository.findById(10L)).willReturn(Optional.of(buildApprovalDoc(10L, drafter)));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.APPROVAL, 10L))
                .willReturn(List.of(fileAttachment));

        // when
        fileService.deleteFile("APPROVAL", 10L);

        // then
        verify(fileAttachmentRepository).existsByFilePathAndIdNotIn(fileAttachment.getFilePath(), List.of(1L));
    }
}
