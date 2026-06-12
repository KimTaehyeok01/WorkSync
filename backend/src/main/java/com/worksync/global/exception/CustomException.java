package com.worksync.global.exception;

import lombok.Getter;

// 비지니스 예외 처리를 일관되게 처리하기 위한 사용자 정의 예외클래스
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
