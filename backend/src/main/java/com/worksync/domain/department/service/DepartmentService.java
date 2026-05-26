package com.worksync.domain.department.service;

import com.worksync.domain.department.dto.DepartmentRequest;
import com.worksync.domain.department.dto.DepartmentResponse;
import com.worksync.domain.department.entity.Department;
import com.worksync.domain.department.repository.DepartmentRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) //읽기전용 나중에 쓰기작업 들어가는 곳에만 사용할때 쓰고 사용
public class DepartmentService {

  private final DepartmentRepository departmentRepository;

  // 전체 부서목록조회
  public List<DepartmentResponse> findDept(){
    return departmentRepository.findAll()
            .stream()
            .map(DepartmentResponse::from).collect(Collectors.toList()); //entity -> Dto로 변환
  }

  // 단건 부서조회
  public DepartmentResponse findByDept(Long id){
    Department dept = departmentRepository.findById(id)
            // 있는 id 면 반환, 없는 id면 던져 실행 정지
            .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
    return DepartmentResponse.from(dept);
  }

  // 부서 생성
  @Transactional //이걸로 읽기전용 해제후 작성가능
  public DepartmentResponse createDept(DepartmentRequest request){
    if (departmentRepository.existsByName(request.getName())) {
      throw new CustomException(ErrorCode.DUPLICATE_DEPARTMENT_NAME);
    }
    Department dept = Department.builder()
            .name(request.getName())
            .build();
    return DepartmentResponse.from(departmentRepository.save(dept)); //db저장후 반환
  }

  // 부서명 수정
  @Transactional
  public DepartmentResponse updateDept(Long id, DepartmentRequest request){
    Department dept = departmentRepository.findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
    if (departmentRepository.existsByName(request.getName())) {
      throw new CustomException(ErrorCode.DUPLICATE_DEPARTMENT_NAME);
    }
    dept.updateName(request.getName()); //entity 메서드로 이름변겯
    return DepartmentResponse.from(dept);
  }

  // 부서 삭제
  @Transactional
  public void deleteDept(Long id){
    Department dept = departmentRepository.findById(id)
            // 존재여부 확인후 삭제 없으면 404
            .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
    departmentRepository.delete(dept);
  }
}
