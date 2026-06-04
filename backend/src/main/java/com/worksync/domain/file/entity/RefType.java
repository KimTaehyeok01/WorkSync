package com.worksync.domain.file.entity;

import java.util.Arrays;

public enum RefType {
    APPROVAL(1L, "approval"),
    TASK(2L, "task"),
    CHAT(3L, "chat"),
    ORGANIZATION(4L, "organization"),
    BOARD(5L, "board");

    private final Long refId;
    private final String refName;

    RefType(Long refId, String refName) {
        this.refId = refId;
        this.refName = refName;
    }

    public Long getRefId() {
        return refId;
    }

    public static RefType fromTypeName(String refName) {
        return Arrays.stream(values())
                .filter(ref -> ref.refName.equalsIgnoreCase(refName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(("존재하지 메뉴입니다.")));
    }
}
