package com.worksync.domain.file.entity;

import java.util.Arrays;

public enum RefType {
    APPROVAL,
    TASK,
    CHAT,
    POST,
    DEPARTMENT,
    EMPLOYEE,
    BOARD;

    public static RefType fromTypeName(String refName) {
        return Arrays.stream(values())
                .filter(ref -> ref.name().equalsIgnoreCase(refName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("RefType 타입변경 에러"));
    }
}
