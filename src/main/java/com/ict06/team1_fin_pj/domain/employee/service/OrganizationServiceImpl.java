package com.ict06.team1_fin_pj.domain.employee.service;

import com.ict06.team1_fin_pj.common.dto.employee.DepartmentTreeDto;
import com.ict06.team1_fin_pj.common.dto.employee.OrgEmployeeDto;
import com.ict06.team1_fin_pj.domain.employee.entity.DepartmentEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.employee.repository.DepartmentRepository;
import com.ict06.team1_fin_pj.domain.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/*
 * 조직도 서비스 구현체
 *
 * 핵심 역할:
 * 1. DEPARTMENT 데이터를 조직도 트리 형태로 변환
 * 2. 특정 부서의 사원 목록 조회
 * 3. 전체 조직도 사원 목록 조회
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationServiceImpl implements OrganizationService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    /*
     * 전체 부서 조직도 조회
     */
    @Override
    public List<DepartmentTreeDto> getDepartmentTree() {

        List<DepartmentEntity> departments = departmentRepository.findAll();
        Map<Integer, DepartmentTreeDto> dtoMap = new HashMap<>();

        for (DepartmentEntity department : departments) {
            DepartmentTreeDto dto = DepartmentTreeDto.builder()
                    .deptId(department.getDeptId())
                    .deptName(department.getDeptName())
                    .children(new ArrayList<>())
                    .build();

            dtoMap.put(department.getDeptId(), dto);
        }

        List<DepartmentTreeDto> rootDepartments = new ArrayList<>();

        for (DepartmentEntity department : departments) {
            DepartmentTreeDto currentDto = dtoMap.get(department.getDeptId());

            if (department.getParentDept() == null) {
                rootDepartments.add(currentDto);
            } else {
                Integer parentDeptId = department.getParentDept().getDeptId();
                DepartmentTreeDto parentDto = dtoMap.get(parentDeptId);

                if (parentDto != null) {
                    parentDto.addChild(currentDto);
                }
            }
        }

        return rootDepartments;
    }

    /*
     * 특정 부서의 사원 목록 조회
     */
    @Override
    public List<OrgEmployeeDto> getEmployeesByDepartment(Integer deptId) {

        boolean hasChildren = departmentRepository.existsByParentDept_DeptId(deptId);

        List<EmpEntity> employees;

        if (hasChildren) {
            employees = employeeRepository.findOrgEmployeesByParentDepartmentId(deptId);
        } else {
            employees = employeeRepository.findOrgEmployeesByDepartmentId(deptId);
        }

        return convertToOrgEmployeeDtos(employees);
    }

    /*
     * 전체 조직도 사원 목록 조회
     *
     * 조직도에서 "전체 조직 보기"를 클릭했을 때 사용한다.
     *
     * 조회 기준:
     * - 삭제 처리되지 않은 사원
     * - 퇴사 상태가 아닌 사원
     *
     * 정렬은 EmployeeRepository의 findAllOrgEmployees() 쿼리에서 처리한다.
     */
    @Override
    public List<OrgEmployeeDto> getAllEmployees() {
        List<EmpEntity> employees = employeeRepository.findAllOrgEmployees();

        return convertToOrgEmployeeDtos(employees);
    }

    /*
     * EmpEntity 목록을 OrgEmployeeDto 목록으로 변환한다.
     *
     * 특정 부서 조회와 전체 조직 조회에서 같은 변환 로직을 사용하므로
     * 중복을 줄이기 위해 private 메서드로 분리했다.
     */
    private List<OrgEmployeeDto> convertToOrgEmployeeDtos(List<EmpEntity> employees) {

        return employees.stream()
                .map(emp -> OrgEmployeeDto.builder()

                        // 사번
                        .empNo(emp.getEmpNo())

                        // 사원 아이디
                        .empId(emp.getEmpId())

                        // 이름
                        .name(emp.getName())

                        // 부서 정보
                        .deptId(emp.getDepartment().getDeptId())
                        .deptName(emp.getDepartment().getDeptName())

                        // 직급 정보
                        .positionId(emp.getPosition().getPositionId())
                        .positionName(emp.getPosition().getPositionName())

                        // 재직 상태
                        .status(emp.getStatus())

                        // 연락처 정보
                        .email(emp.getEmail())
                        .phone(emp.getPhone())

                        // 프로필 이미지
                        .profileImg(emp.getProfileImg())

                        // 입사일
                        .hireDate(emp.getHireDate())

                        .build())

                .toList();
    }
}