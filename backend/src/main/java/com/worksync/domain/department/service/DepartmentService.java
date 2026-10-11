package com.worksync.domain.department.service;

import com.worksync.domain.department.dto.DepartmentDto;
import com.worksync.domain.department.entity.Department;
import com.worksync.domain.department.repository.DepartmentRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) //읽기전용 나중에 쓰기작업 들어가는 곳에만 사용할때 쓰고 사용
public class DepartmentService {

  private final DepartmentRepository departmentRepository;

  // 전체 부서목록조회
  public List<DepartmentDto.Response> findDept(){
    return departmentRepository.findAll()
            .stream()
            .map(DepartmentDto.Response::from).collect(Collectors.toList()); //entity -> Dto로 변환
  }

  // 단건 부서조회
  public DepartmentDto.Response findByDept(Long id){
    Department dept = departmentRepository.findById(id)
            // 있는 id 면 반환, 없는 id면 던져 실행 정지
            .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
    return DepartmentDto.Response.from(dept);
  }

  // 부서 생성
  @Transactional //이걸로 읽기전용 해제후 작성가능
  public DepartmentDto.Response createDept(DepartmentDto.Request request){
    if (departmentRepository.existsByName(request.getName())) {
      throw new CustomException(ErrorCode.DUPLICATE_DEPARTMENT_NAME);
    }
    Department dept = Department.builder()
            .name(request.getName())
            .parent(findParentOrNull(request.getParentId()))
            .build();
    return DepartmentDto.Response.from(departmentRepository.save(dept)); //db저장후 반환
  }

  // 부서명·상위부서 수정
  @Transactional
  public DepartmentDto.Response updateDept(Long id, DepartmentDto.Request request){
    Department dept = departmentRepository.findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
    // 이름이 그대로면 중복 검사 생략 (상위부서만 바꾸는 수정 허용), 다른 부서와 같은 이름이면 거부
    if (!dept.getName().equals(request.getName())
            && departmentRepository.existsByName(request.getName())) {
      throw new CustomException(ErrorCode.DUPLICATE_DEPARTMENT_NAME);
    }
    Department parent = findParentOrNull(request.getParentId());
    validateParent(dept, parent); // 검증 통과 후에만 entity 를 변경
    dept.updateName(request.getName()); //entity 메서드로 이름변겯
    dept.updateParent(parent);
    return DepartmentDto.Response.from(dept);
  }

  // 부서 삭제
  @Transactional
  public void deleteDept(Long id){
    Department dept = departmentRepository.findById(id)
            // 존재여부 확인후 삭제 없으면 404
            .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
    departmentRepository.delete(dept);
  }

  // parentId 로 상위 부서 조회 (null 이면 최상위이므로 null, 없는 id 면 404)
  private Department findParentOrNull(Long parentId){
    if (parentId == null) {
      return null;
    }
    return departmentRepository.findById(parentId)
            .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
  }

  // 자기 자신 또는 자기 후손을 상위 부서로 지정하는 순환 참조 방지
  private void validateParent(Department dept, Department parent){
    Set<Long> visited = new HashSet<>(); // 기존 데이터에 순환이 있어도 무한루프 방지
    for (Department cur = parent; cur != null; cur = cur.getParent()) {
      if (cur.getId().equals(dept.getId()) || !visited.add(cur.getId())) {
        throw new CustomException(ErrorCode.INVALID_DEPARTMENT_PARENT);
      }
    }
  }
}
