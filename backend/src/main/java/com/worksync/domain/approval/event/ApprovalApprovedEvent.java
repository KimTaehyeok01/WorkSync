package com.worksync.domain.approval.event;

// 결재 문서가 최종 승인됐을 때 발행되는 이벤트
// 구독 측(예: leave)이 formType으로 자기 문서인지 판별 후 후속 처리(연차 차감 등)를 수행한다.
public record ApprovalApprovedEvent(Long docId, String formType) {
}
