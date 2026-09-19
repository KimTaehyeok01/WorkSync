// AuditLogService 단위 테스트
package com.worksync.domain.audit.service;

import com.worksync.domain.audit.dto.AuditLogDto;
import com.worksync.domain.audit.entity.AuditLog;
import com.worksync.domain.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/** AuditLogService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @DisplayName("카테고리/기간/키워드 필터를 모두 지정하면 조건에 맞는 로그를 조회한다")
    @Test
    void getLogs_success_withAllFilters() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log = AuditLog.builder()
                .id(1L).actorId(1L).actorName("김철수").action("게시글 작성")
                .targetType("APPROVAL").targetId(10L)
                .clientIp("127.0.0.1").userAgent("Mozilla").build();
        given(auditLogRepository.search(eq("APPROVAL"), any(), eq("김철수"), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(log), pageable, 1));

        // when
        Page<AuditLogDto.Response> result = auditLogService.getLogs("APPROVAL", "week", "김철수", pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getActorName()).isEqualTo("김철수");

        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(auditLogRepository).search(eq("APPROVAL"), fromCaptor.capture(), eq("김철수"), eq(pageable));
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.now().minusDays(6).atStartOfDay());
    }

    @DisplayName("카테고리와 키워드가 빈 값이면 null로 정규화되어 조회된다")
    @Test
    void getLogs_blankCategoryAndKeyword_normalizedToNull() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        given(auditLogRepository.search(isNull(), isNull(), isNull(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        Page<AuditLogDto.Response> result = auditLogService.getLogs("", null, "   ", pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(auditLogRepository).search(isNull(), isNull(), isNull(), eq(pageable));
    }

    @DisplayName("기간이 today이면 오늘 00시부터 조회한다")
    @Test
    void getLogs_periodToday_resolvesFromStartOfToday() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        given(auditLogRepository.search(any(), any(), any(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        auditLogService.getLogs(null, "today", null, pageable);

        // then
        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(auditLogRepository).search(isNull(), fromCaptor.capture(), isNull(), eq(pageable));
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.now().atStartOfDay());
    }

    @DisplayName("기간이 month이면 이번달 1일 00시부터 조회한다")
    @Test
    void getLogs_periodMonth_resolvesFromFirstDayOfMonth() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        given(auditLogRepository.search(any(), any(), any(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        auditLogService.getLogs(null, "month", null, pageable);

        // then
        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(auditLogRepository).search(isNull(), fromCaptor.capture(), isNull(), eq(pageable));
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.now().withDayOfMonth(1).atStartOfDay());
    }

    @DisplayName("기간이 all이면 전체 기간을 조회한다 (from = null)")
    @Test
    void getLogs_periodAll_resolvesFromNull() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        given(auditLogRepository.search(isNull(), isNull(), isNull(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        auditLogService.getLogs(null, "all", null, pageable);

        // then
        verify(auditLogRepository).search(isNull(), isNull(), isNull(), eq(pageable));
    }

    @DisplayName("정의되지 않은 기간 값이면 전체 기간을 조회한다 (from = null)")
    @Test
    void getLogs_periodUnknownValue_resolvesFromNull() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        given(auditLogRepository.search(isNull(), isNull(), isNull(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        auditLogService.getLogs(null, "year", null, pageable);

        // then
        verify(auditLogRepository).search(isNull(), isNull(), isNull(), eq(pageable));
    }

    @DisplayName("감사 로그 요약 통계를 조회한다")
    @Test
    void getSummary_success() {
        // given
        given(auditLogRepository.count()).willReturn(120L);
        given(auditLogRepository.countByCreatedAtGreaterThanEqual(any())).willReturn(15L);
        given(auditLogRepository.countByAction("로그인 실패")).willReturn(2L);
        given(auditLogRepository.countByTargetType("APPROVAL")).willReturn(8L);

        // when
        AuditLogDto.Summary result = auditLogService.getSummary();

        // then
        assertThat(result.getTotalCount()).isEqualTo(120L);
        assertThat(result.getTodayCount()).isEqualTo(15L);
        assertThat(result.getLoginFailCount()).isEqualTo(2L);
        assertThat(result.getApprovalCount()).isEqualTo(8L);
    }

    @DisplayName("감사 로그를 정상적으로 기록한다")
    @Test
    void log_success() {
        // given
        given(auditLogRepository.save(any(AuditLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        auditLogService.log(1L, "김철수", "로그인 성공", "AUTH", 1L, "127.0.0.1", "Mozilla");

        // then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getActorId()).isEqualTo(1L);
        assertThat(saved.getActorName()).isEqualTo("김철수");
        assertThat(saved.getAction()).isEqualTo("로그인 성공");
        assertThat(saved.getTargetType()).isEqualTo("AUTH");
        assertThat(saved.getTargetId()).isEqualTo(1L);
        assertThat(saved.getClientIp()).isEqualTo("127.0.0.1");
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla");
    }
}
