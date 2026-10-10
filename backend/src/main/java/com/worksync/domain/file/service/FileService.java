package com.worksync.domain.file.service;

import com.worksync.domain.approval.entity.ApprovalDoc;
import com.worksync.domain.approval.repository.ApprovalDocRepository;
import com.worksync.domain.board.entity.Board;
import com.worksync.domain.board.entity.BoardType;
import com.worksync.domain.board.entity.Post;
import com.worksync.domain.board.repository.PostRepository;
import com.worksync.domain.chat.repository.ChatMemberRepository;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.file.dto.FileDto;
import com.worksync.domain.file.entity.FileAttachment;
import com.worksync.domain.file.entity.RefType;
import com.worksync.domain.file.repository.FileAttachmentRepository;
import com.worksync.domain.mail.entity.Mail;
import com.worksync.domain.mail.repository.MailRecipientRepository;
import com.worksync.domain.mail.repository.MailRepository;
import com.worksync.domain.task.repository.TaskRepository;
import com.worksync.domain.task.service.TaskAccessPolicy;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import com.worksync.global.security.CurrentEmployeeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;
    // 첨부가 달린 원글의 권한을 확인하는 데 쓴다
    private final TaskRepository taskRepository;
    private final ApprovalDocRepository approvalDocRepository;
    private final PostRepository postRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MailRepository mailRepository;
    private final MailRecipientRepository mailRecipientRepository;
    private final TaskAccessPolicy taskAccessPolicy;
    private final CurrentEmployeeProvider currentEmployeeProvider;

    private final RestClient restClient = RestClient.create();

    @Value("${app.supabase.url}")
    private String supabaseUrl;

    @Value("${app.supabase.service-role-key}")
    private String serviceRoleKey;

    private static final String BUCKET = "WorkSync";

    // refId를 제외한 초기 파일 업로드
    @Transactional
    public FileDto.UploadResponse upload(MultipartFile file) {
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

        return FileDto.UploadResponse.builder()
                .filePath(publicUrl)
                .originalName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .build();
    }

    // 최종 저장시 refId 추가
    @Transactional
    public FileDto.UploadResponse updateRefId(Long uploaderId, FileDto.SaveRequest request) {
        // 업로드 사원 조회
        Employee uploader = employeeRepository.findById(uploaderId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        // String refType -> RefType refType 타입 변환
        RefType refTypeName = RefType.fromTypeName(request.getRefType());

        // 원글을 고칠 수 있는 사람만 첨부를 추가한다 — 판정 대상은 파일에 업로더로 기록되는 직원 본인
        if (!canAttach(refTypeName, request.getRefId(), uploader)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 이 서비스가 업로드한 파일의 공개 URL 만 받는다 — 남의 파일·외부 URL 을 끼워 넣어 나중에 삭제시키는 것을 막는다
        validateFilePath(request.getFilePath());

        // DB에 파일 정보 저장
        FileAttachment fileAttachment = FileAttachment.builder()
                .uploader(uploader)
                .filePath(request.getFilePath())
                .originalName(request.getOriginalName())
                .fileSize(request.getFileSize())
                .mimeType(request.getMimeType())
                .refType(refTypeName)
                .refId(request.getRefId())
                .build();

        // refId 최종 저장
        FileAttachment saved = fileAttachmentRepository.save(fileAttachment);

        // (WebSocket) 저장 완료 후 채팅방 실시간 푸쉬
        if (refTypeName == RefType.CHAT) {
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + request.getRefId() + "/files",
                    FileDto.UploadResponse.from(saved)
            );
        }

        return FileDto.UploadResponse.from(saved);
    }

    // 파일 단건 조회
    public FileDto.UploadResponse findFileId(Long id) {
        FileAttachment file = fileAttachmentRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

        // 원글을 볼 수 없는 사람에게는 파일의 존재 여부도 알려주지 않는다
        if (!canView(file.getRefType(), file.getRefId(), currentEmployeeProvider.getCurrentEmployee())) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        return FileDto.UploadResponse.from(file);
    }

    // 첨부 위치별 파일 목록 조회
    public List<FileDto.SaveRequest> findByRef(String refType, Long refId) {
        // String refType -> RefType refType 타입 변환
        RefType refTypeName = RefType.fromTypeName(refType);

        // 원글을 볼 수 있는 사람만 첨부 목록을 조회한다
        if (!canView(refTypeName, refId, currentEmployeeProvider.getCurrentEmployee())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return fileAttachmentRepository.findByRefTypeAndRefId(refTypeName, refId)
                .stream()
                .map(FileDto.SaveRequest::from)
                .collect(Collectors.toList());
    }

    // 파일 삭제
    @Transactional
    public void deleteFile(String refType, Long refId) {

        // String refType -> RefType refType 타입 변환
        RefType refTypeName = RefType.fromTypeName(refType);

        // 원글을 고칠 수 있는 사람만 첨부를 삭제한다
        if (!canDelete(refTypeName, refId, currentEmployeeProvider.getCurrentEmployee())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 파일 불러오기
        List<FileAttachment> files = fileAttachmentRepository.findByRefTypeAndRefId(refTypeName, refId);
        List<Long> deletingIds = files.stream().map(FileAttachment::getId).toList();

        // 파일 반복문으로 돌리면서 삭제
        for (FileAttachment file : files) {
            // 같은 스토리지 파일을 다른 첨부가 아직 쓰고 있으면 스토리지 객체는 두고 DB 행만 지운다
            if (!fileAttachmentRepository.existsByFilePathAndIdNotIn(file.getFilePath(), deletingIds)) {
                deleteStorageObject(file);
            }

            // DB에 있으면 삭제
            fileAttachmentRepository.delete(file);
        }
    }

    // Supabase Storage 객체 삭제 — 실패해도 DB 삭제는 진행한다
    private void deleteStorageObject(FileAttachment file) {
        // 저장된 URL에서 objectPath 추출
        String objectPath = file.getFilePath().replace(publicUrlPrefix(), "");

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
    }

    private String publicUrlPrefix() {
        return supabaseUrl + "/storage/v1/object/public/" + BUCKET + "/";
    }

    // 저장 요청의 filePath 검증 — 우리 공개 URL 접두사로 시작하고, 뒤의 objectPath 에 경로 조작 문자가 없어야 한다
    private void validateFilePath(String filePath) {
        String prefix = publicUrlPrefix();
        if (filePath == null || !filePath.startsWith(prefix)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        String objectPath = filePath.substring(prefix.length());
        if (objectPath.isEmpty() || objectPath.contains("/") || objectPath.contains("\\") || objectPath.contains("..")) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 조회 권한 — 첨부가 달린 원글을 볼 수 있는 사람. 규칙은 각 도메인의 조회 규칙을 따른다
    // 원글이 없으면 권한이 없는 것과 똑같이 false 로 답해 원글의 존재 여부를 드러내지 않는다
    private boolean canView(RefType refType, Long refId, Employee me) {
        // refId 가 없으면 원글을 특정할 수 없으므로 거부 (조회·추가·삭제 공통)
        if (refId == null) {
            return false;
        }
        return switch (refType) {
            case TASK -> taskRepository.findById(refId)
                    .map(task -> taskAccessPolicy.canView(task, me))
                    .orElse(false);
            case APPROVAL -> approvalDocRepository.findWithDetailsById(refId)
                    .map(doc -> isDrafter(doc, me) || isOnApprovalLine(doc, me))
                    .orElse(false);
            case POST -> postRepository.findById(refId)
                    .map(post -> canViewPost(post, me))
                    .orElse(false);
            case CHAT -> chatMemberRepository.existsByRoomIdAndEmployeeId(refId, me.getId());
            case EMPLOYEE -> true;
            case BOARD, DEPARTMENT -> isAdmin(me);
            case MAIL -> mailRepository.findById(refId)
                    .map(mail -> canViewMail(mail, me))
                    .orElse(false);
        };
    }

    // 추가 권한 — 원글을 고칠 수 있는 사람 (채팅은 참여자)
    private boolean canAttach(RefType refType, Long refId, Employee me) {
        if (refId == null) {
            return false;
        }
        return switch (refType) {
            case TASK -> taskRepository.findById(refId)
                    .map(task -> taskAccessPolicy.isCreator(task, me.getId()))
                    .orElse(false);
            case APPROVAL -> approvalDocRepository.findById(refId)
                    .map(doc -> isDrafter(doc, me))
                    .orElse(false);
            case POST -> postRepository.findById(refId)
                    .map(post -> post.getAuthor().getId().equals(me.getId()))
                    .orElse(false);
            case CHAT -> chatMemberRepository.existsByRoomIdAndEmployeeId(refId, me.getId());
            case EMPLOYEE, BOARD, DEPARTMENT -> isAdmin(me);
            // 메일 첨부는 보낸 사람만 바꾼다 — 보낸편지함에서 지운 뒤에는 그 메일을 볼 수 없으니 바꿀 수도 없다
            case MAIL -> mailRepository.findById(refId)
                    .map(mail -> isSenderWithMail(mail, me))
                    .orElse(false);
        };
    }

    // 삭제 권한 — 추가 권한과 같되, 채팅 첨부는 누구도 지울 수 없다
    private boolean canDelete(RefType refType, Long refId, Employee me) {
        return refType != RefType.CHAT && canAttach(refType, refId, me);
    }

    private boolean isDrafter(ApprovalDoc doc, Employee me) {
        return doc.getDrafter().getId().equals(me.getId());
    }

    // 결재선에 있는 사람 — 검토·승인자와 참조인 모두 포함
    private boolean isOnApprovalLine(ApprovalDoc doc, Employee me) {
        return doc.getApprovalLines().stream()
                .anyMatch(line -> line.getApprover().getId().equals(me.getId()));
    }

    // 부서게시판 글은 관리자이거나 그 부서 소속일 때만, 그 외 게시판 글은 누구나 (PostService.getPosts 와 같은 규칙)
    private boolean canViewPost(Post post, Employee me) {
        Board board = post.getBoard();
        if (board.getBoardType() != BoardType.DEPARTMENT || board.getDepartment() == null) {
            return true;
        }
        return isAdmin(me) || (me.getDepartment() != null
                && board.getDepartment().getId().equals(me.getDepartment().getId()));
    }

    // 보낸 사람이면서 보낸편지함에서 지우지 않은 경우 (MailService.getDetail 의 sentByMe 와 같은 판정)
    private boolean isSenderWithMail(Mail mail, Employee me) {
        return mail.getSender().getId().equals(me.getId()) && !mail.getIsSenderDeleted();
    }

    // 메일 조회 — 보낸 사람(지우지 않음) 이거나, 자기 쪽에서 지우지 않은 수신자 (MailService.getDetail 과 같은 판정)
    private boolean canViewMail(Mail mail, Employee me) {
        if (isSenderWithMail(mail, me)) {
            return true;
        }
        return mailRecipientRepository.findByMailIdAndRecipientId(mail.getId(), me.getId())
                .filter(recipient -> !recipient.getIsDeleted())
                .isPresent();
    }

    private boolean isAdmin(Employee employee) {
        return employee.getRole() == EmployeeRole.ADMIN;
    }
}
