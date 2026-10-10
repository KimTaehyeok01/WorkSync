// 직원·게시글·업무·결재·일정·메일을 한 번에 찾는 통합 검색 서비스
package com.worksync.domain.search.service;

import com.worksync.domain.approval.repository.ApprovalDocRepository;
import com.worksync.domain.board.entity.BoardType;
import com.worksync.domain.board.repository.PostRepository;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.mail.repository.MailRecipientRepository;
import com.worksync.domain.mail.repository.MailRepository;
import com.worksync.domain.schedule.entity.ScheduleScope;
import com.worksync.domain.schedule.repository.ScheduleRepository;
import com.worksync.domain.search.dto.SearchDto;
import com.worksync.domain.task.repository.TaskRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

/** 통합 검색 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

	private static final int MIN_KEYWORD_LENGTH = 2;
	// 종류별 최대 결과 수
	private static final Pageable LIMIT = PageRequest.of(0, 5);

	private final EmployeeRepository employeeRepository;
	private final PostRepository postRepository;
	private final TaskRepository taskRepository;
	private final ApprovalDocRepository approvalDocRepository;
	private final ScheduleRepository scheduleRepository;
	private final MailRepository mailRepository;
	private final MailRecipientRepository mailRecipientRepository;

	// 내가 볼 수 있는 범위 안에서만 찾는다 — 각 도메인의 조회 권한 규칙을 그대로 따른다
	public SearchDto.Response search(Long myId, String rawKeyword) {
		String keyword = rawKeyword == null ? "" : rawKeyword.trim();
		if (keyword.length() < MIN_KEYWORD_LENGTH) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
		Employee me = employeeRepository.findById(myId)
				.orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
		boolean isAdmin = me.getRole() == EmployeeRole.ADMIN;
		Long departmentId = me.getDepartment() != null ? me.getDepartment().getId() : null;

		return SearchDto.Response.builder()
				.employees(searchEmployees(keyword))
				.posts(searchPosts(keyword, isAdmin, departmentId))
				.tasks(searchTasks(keyword, isAdmin, departmentId, myId))
				.approvals(searchApprovals(keyword, myId))
				.schedules(searchSchedules(keyword, myId, departmentId))
				.mails(searchMails(keyword, myId))
				.build();
	}

	private List<SearchDto.Item> searchEmployees(String keyword) {
		return employeeRepository.findByNameContaining(keyword, LIMIT).stream()
				.filter(employee -> employee.getStatus() != EmployeeStatus.INACTIVE)
				.map(employee -> SearchDto.Item.builder()
						.id(employee.getId())
						.title(employee.getName())
						.description(joinNonNull(
								employee.getDepartment() != null ? employee.getDepartment().getName() : null,
								employee.getEmail()))
						.build())
				.toList();
	}

	private List<SearchDto.Item> searchPosts(String keyword, boolean isAdmin, Long departmentId) {
		return postRepository.searchVisible(keyword, BoardType.DEPARTMENT, isAdmin, departmentId, LIMIT).stream()
				.map(post -> SearchDto.Item.builder()
						.id(post.getId())
						.parentId(post.getBoard().getId())
						.title(post.getTitle())
						.description(joinNonNull(post.getBoard().getName(), post.getAuthor().getName()))
						.dateAt(post.getCreatedAt())
						.build())
				.toList();
	}

	private List<SearchDto.Item> searchTasks(String keyword, boolean isAdmin, Long departmentId, Long myId) {
		return taskRepository.searchVisible(keyword, isAdmin, departmentId, myId, LIMIT).stream()
				.map(task -> SearchDto.Item.builder()
						.id(task.getId())
						.title(task.getTitle())
						.description(joinNonNull(
								task.getStatus().name(),
								task.getAssignee() != null ? task.getAssignee().getName() : null))
						.dateAt(task.getCreatedAt())
						.build())
				.toList();
	}

	private List<SearchDto.Item> searchApprovals(String keyword, Long myId) {
		return approvalDocRepository.searchMine(keyword, myId, LIMIT).stream()
				.map(doc -> SearchDto.Item.builder()
						.id(doc.getId())
						.title(doc.getTitle())
						.description(joinNonNull(doc.getStatus().name(), doc.getDrafter().getName()))
						.dateAt(doc.getCreatedAt())
						.build())
				.toList();
	}

	private List<SearchDto.Item> searchSchedules(String keyword, Long myId, Long departmentId) {
		return scheduleRepository.searchVisible(
						myId, departmentId, keyword,
						ScheduleScope.PERSONAL, ScheduleScope.DEPARTMENT, ScheduleScope.COMPANY, LIMIT)
				.stream()
				.map(schedule -> SearchDto.Item.builder()
						.id(schedule.getId())
						.title(schedule.getTitle())
						.description(joinNonNull(schedule.getOwner().getName(), schedule.getLocation()))
						.dateAt(schedule.getStartAt())
						.build())
				.toList();
	}

	// 받은 메일과 보낸 메일을 합쳐 최신순 5건
	private List<SearchDto.Item> searchMails(String keyword, Long myId) {
		Stream<SearchDto.Item> received = mailRecipientRepository.findInbox(myId, false, keyword, LIMIT).stream()
				.map(recipient -> SearchDto.Item.builder()
						.id(recipient.getMail().getId())
						.title(recipient.getMail().getSubject())
						.description("받은 메일 · " + recipient.getMail().getSender().getName())
						.dateAt(recipient.getMail().getCreatedAt())
						.build());
		Stream<SearchDto.Item> sent = mailRepository.findSent(myId, keyword, LIMIT).stream()
				.map(mail -> SearchDto.Item.builder()
						.id(mail.getId())
						.title(mail.getSubject())
						.description("보낸 메일")
						.dateAt(mail.getCreatedAt())
						.build());

		return Stream.concat(received, sent)
				.sorted((a, b) -> b.getDateAt().compareTo(a.getDateAt()))
				.limit(LIMIT.getPageSize())
				.toList();
	}

	private String joinNonNull(String first, String second) {
		if (first == null) {
			return second;
		}
		return second == null ? first : first + " · " + second;
	}
}
