package com.worksync.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    INVALID_CREDENTIALS(401, "이메일 또는 비밀번호가 올바르지 않습니다."),
    EXPIRED_TOKEN(401, "만료된 토큰입니다."),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다."),
    ACCOUNT_LOCKED(423, "계정이 잠겨 있습니다."),

    // Employee
    EMPLOYEE_NOT_FOUND(404, "존재하지 않는 사원입니다."),
    DUPLICATE_EMAIL(409, "이미 사용 중인 이메일입니다."),
    DUPLICATE_EMP_NO(409, "이미 사용 중인 사원번호입니다."),

    // Department
    DEPARTMENT_NOT_FOUND(404, "존재하지 않는 부서입니다."),

    // Approval
    APPROVAL_DOC_NOT_FOUND(404, "존재하지 않는 결재 문서입니다."),
    APPROVAL_FORM_NOT_FOUND(404, "존재하지 않는 결재 양식입니다."),
    NOT_YOUR_APPROVAL(403, "결재 권한이 없습니다."),
    ALREADY_PROCESSED(400, "이미 처리된 결재입니다."),

    // Task
    TASK_NOT_FOUND(404, "존재하지 않는 업무입니다."),

    // Board / Post
    BOARD_NOT_FOUND(404, "존재하지 않는 게시판입니다."),
    POST_NOT_FOUND(404, "존재하지 않는 게시글입니다."),

    // Chat
    CHAT_ROOM_NOT_FOUND(404, "존재하지 않는 채팅방입니다."),
    NOT_CHAT_MEMBER(403, "채팅방 멤버가 아닙니다."),

    // File
    FILE_NOT_FOUND(404, "존재하지 않는 파일입니다."),
    FILE_UPLOAD_FAILED(500, "파일 업로드에 실패했습니다."),

    // Leave
    LEAVE_REQUEST_NOT_FOUND(404, "존재하지 않는 휴가 신청입니다."),
    LEAVE_BALANCE_NOT_FOUND(404, "연차 정보를 찾을 수 없습니다."),
    INSUFFICIENT_LEAVE_BALANCE(400, "연차 잔여일수가 부족합니다."),

    // Common
    FORBIDDEN(403, "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다.");

    private final int status;
    private final String message;
}
