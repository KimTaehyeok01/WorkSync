// DepartmentService 단위 테스트
package com.worksync.domain.department.service;

import com.worksync.domain.department.dto.DepartmentDto;
import com.worksync.domain.department.entity.Department;
import com.worksync.domain.department.repository.DepartmentRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** DepartmentService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private DepartmentDto.Request buildRequest(String name) {
        DepartmentDto.Request request = new DepartmentDto.Request();
        request.setName(name);
        return request;
    }

    private DepartmentDto.Request buildRequest(String name, Long parentId) {
        DepartmentDto.Request request = buildRequest(name);
        request.setParentId(parentId);
        return request;
    }

    @DisplayName("전체 부서 목록을 조회한다")
    @Test
    void findDept_success() {
        // given
        Department dept1 = Department.builder().id(1L).name("개발팀").build();
        Department dept2 = Department.builder().id(2L).name("인사팀").build();
        given(departmentRepository.findAll()).willReturn(List.of(dept1, dept2));

        // when
        List<DepartmentDto.Response> result = departmentService.findDept();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("개발팀");
        assertThat(result.get(1).getName()).isEqualTo("인사팀");
    }

    @DisplayName("부서 단건 조회 - 정상")
    @Test
    void findByDept_success() {
        // given
        Department dept = Department.builder().id(1L).name("개발팀").build();
        given(departmentRepository.findById(1L)).willReturn(Optional.of(dept));

        // when
        DepartmentDto.Response result = departmentService.findByDept(1L);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("개발팀");
    }

    @DisplayName("존재하지 않는 부서를 조회하면 예외가 발생한다")
    @Test
    void findByDept_notFound_throwsDepartmentNotFound() {
        // given
        given(departmentRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> departmentService.findByDept(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @DisplayName("부서를 정상적으로 생성한다")
    @Test
    void createDept_success() {
        // given
        DepartmentDto.Request request = buildRequest("개발팀");
        given(departmentRepository.existsByName("개발팀")).willReturn(false);
        given(departmentRepository.save(any(Department.class)))
                .willAnswer(invocation -> {
                    Department arg = invocation.getArgument(0);
                    return Department.builder().id(1L).name(arg.getName()).build();
                });

        // when
        DepartmentDto.Response result = departmentService.createDept(request);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("개발팀");
    }

    @DisplayName("이미 존재하는 부서명으로 생성하면 예외가 발생한다")
    @Test
    void createDept_duplicateName_throwsDuplicateDepartmentName() {
        // given
        DepartmentDto.Request request = buildRequest("개발팀");
        given(departmentRepository.existsByName("개발팀")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> departmentService.createDept(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_DEPARTMENT_NAME);
        verify(departmentRepository, never()).save(any());
    }

    @DisplayName("부서명을 정상적으로 수정한다")
    @Test
    void updateDept_success() {
        // given
        Department existing = Department.builder().id(1L).name("기존팀").build();
        DepartmentDto.Request request = buildRequest("새이름팀");
        given(departmentRepository.findById(1L)).willReturn(Optional.of(existing));
        given(departmentRepository.existsByName("새이름팀")).willReturn(false);

        // when
        DepartmentDto.Response result = departmentService.updateDept(1L, request);

        // then
        assertThat(result.getName()).isEqualTo("새이름팀");
        assertThat(existing.getName()).isEqualTo("새이름팀");
    }

    @DisplayName("존재하지 않는 부서를 수정하면 예외가 발생한다")
    @Test
    void updateDept_notFound_throwsDepartmentNotFound() {
        // given
        DepartmentDto.Request request = buildRequest("새이름팀");
        given(departmentRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> departmentService.updateDept(999L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @DisplayName("이미 존재하는 부서명으로 수정하면 예외가 발생한다")
    @Test
    void updateDept_duplicateName_throwsDuplicateDepartmentName() {
        // given
        Department existing = Department.builder().id(1L).name("기존팀").build();
        DepartmentDto.Request request = buildRequest("중복팀");
        given(departmentRepository.findById(1L)).willReturn(Optional.of(existing));
        given(departmentRepository.existsByName("중복팀")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> departmentService.updateDept(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_DEPARTMENT_NAME);
        assertThat(existing.getName()).isEqualTo("기존팀");
    }

    @DisplayName("상위 부서를 지정해 하위 부서를 생성한다")
    @Test
    void createDept_withParent_success() {
        // given
        Department parent = Department.builder().id(1L).name("개발본부").build();
        DepartmentDto.Request request = buildRequest("개발팀", 1L);
        given(departmentRepository.existsByName("개발팀")).willReturn(false);
        given(departmentRepository.findById(1L)).willReturn(Optional.of(parent));
        given(departmentRepository.save(any(Department.class)))
                .willAnswer(invocation -> {
                    Department arg = invocation.getArgument(0);
                    return Department.builder().id(2L).name(arg.getName()).parent(arg.getParent()).build();
                });

        // when
        DepartmentDto.Response result = departmentService.createDept(request);

        // then
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getParentId()).isEqualTo(1L);
    }

    @DisplayName("존재하지 않는 상위 부서로 생성하면 예외가 발생한다")
    @Test
    void createDept_parentNotFound_throwsDepartmentNotFound() {
        // given
        DepartmentDto.Request request = buildRequest("개발팀", 999L);
        given(departmentRepository.existsByName("개발팀")).willReturn(false);
        given(departmentRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> departmentService.createDept(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
        verify(departmentRepository, never()).save(any());
    }

    @DisplayName("자기 자신을 상위 부서로 수정하면 예외가 발생한다")
    @Test
    void updateDept_selfAsParent_throwsInvalidDepartmentParent() {
        // given
        Department existing = Department.builder().id(1L).name("개발팀").build();
        DepartmentDto.Request request = buildRequest("개발팀", 1L);
        given(departmentRepository.findById(1L)).willReturn(Optional.of(existing));

        // when & then
        assertThatThrownBy(() -> departmentService.updateDept(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DEPARTMENT_PARENT);
        assertThat(existing.getParent()).isNull();
    }

    @DisplayName("자기 하위(후손) 부서를 상위 부서로 수정하면 순환이므로 예외가 발생한다")
    @Test
    void updateDept_descendantAsParent_throwsInvalidDepartmentParent() {
        // given: 본부(1) <- 개발팀(2) <- 백엔드팀(3)
        Department root = Department.builder().id(1L).name("본부").build();
        Department child = Department.builder().id(2L).name("개발팀").parent(root).build();
        Department grandChild = Department.builder().id(3L).name("백엔드팀").parent(child).build();
        DepartmentDto.Request request = buildRequest("본부", 3L);
        given(departmentRepository.findById(1L)).willReturn(Optional.of(root));
        given(departmentRepository.findById(3L)).willReturn(Optional.of(grandChild));

        // when & then
        assertThatThrownBy(() -> departmentService.updateDept(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DEPARTMENT_PARENT);
        assertThat(root.getParent()).isNull();
    }

    @DisplayName("부서명은 그대로 두고 상위 부서만 변경한다")
    @Test
    void updateDept_sameNameChangeParent_success() {
        // given
        Department newParent = Department.builder().id(1L).name("개발본부").build();
        Department existing = Department.builder().id(2L).name("개발팀").build();
        DepartmentDto.Request request = buildRequest("개발팀", 1L);
        given(departmentRepository.findById(2L)).willReturn(Optional.of(existing));
        given(departmentRepository.findById(1L)).willReturn(Optional.of(newParent));

        // when
        DepartmentDto.Response result = departmentService.updateDept(2L, request);

        // then
        assertThat(result.getName()).isEqualTo("개발팀");
        assertThat(result.getParentId()).isEqualTo(1L);
        assertThat(existing.getParent()).isSameAs(newParent);
        verify(departmentRepository, never()).existsByName(any());
    }

    @DisplayName("상위 부서를 null 로 수정하면 최상위 부서로 해제된다")
    @Test
    void updateDept_nullParent_clearsParent() {
        // given
        Department parent = Department.builder().id(1L).name("개발본부").build();
        Department existing = Department.builder().id(2L).name("개발팀").parent(parent).build();
        DepartmentDto.Request request = buildRequest("개발팀", null);
        given(departmentRepository.findById(2L)).willReturn(Optional.of(existing));

        // when
        DepartmentDto.Response result = departmentService.updateDept(2L, request);

        // then
        assertThat(result.getParentId()).isNull();
        assertThat(existing.getParent()).isNull();
    }

    @DisplayName("부서를 정상적으로 삭제한다")
    @Test
    void deleteDept_success() {
        // given
        Department existing = Department.builder().id(1L).name("개발팀").build();
        given(departmentRepository.findById(1L)).willReturn(Optional.of(existing));

        // when
        departmentService.deleteDept(1L);

        // then
        verify(departmentRepository).delete(existing);
    }

    @DisplayName("존재하지 않는 부서를 삭제하면 예외가 발생한다")
    @Test
    void deleteDept_notFound_throwsDepartmentNotFound() {
        // given
        given(departmentRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> departmentService.deleteDept(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }
}
