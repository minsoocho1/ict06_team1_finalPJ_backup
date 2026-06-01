package com.ict06.team1_fin_pj.domain.employee.service;

import com.ict06.team1_fin_pj.common.dto.employee.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/*
 * 인사관리 서비스 인터페이스
 *
 * Controller는 이 인터페이스를 통해 인사관리 기능을 호출한다.
 * 실제 동작 코드는 AdEmployeeServiceImpl 클래스에 작성되어 있다.
 */
public interface AdEmployeeService {

    // 사원 목록 조회
    // 검색 조건 DTO를 받아 조건에 맞는 사원 목록을 반환한다.
    Page<EmployeeListDto> findEmployees(
            EmployeeSearchConditionDto conditionDto,
            Pageable pageable
    );

    /*
     * 전체 부서 목록 조회
     *
     * 사원 목록 검색 필터에서 사용할 수 있다.
     * 현재 목록 화면은 본부/팀 분리 전 방식처럼 전체 부서를 보여준다.
     */
    List<HrSelectOptionDto> findDepartments();

    /*
     * 본부 목록 조회
     *
     * 사원 등록/수정 화면의 첫 번째 select 박스에 사용한다.
     *
     * 기준:
     * - DEPARTMENT.parent_dept_id가 null인 데이터
     * - 예: 경영본부, 개발본부
     */
    List<HrSelectOptionDto> findParentDepartments();

    /*
     * 선택한 본부에 속한 팀 목록 조회
     *
     * 사원 등록/수정 화면의 두 번째 select 박스에 사용한다.
     *
     * 예:
     * parentDeptId = 1
     * → 경영본부 아래의 경영지원팀, 인사팀 조회
     */
    List<HrSelectOptionDto> findTeamsByParentDeptId(Integer parentDeptId);

    // 직급 select 박스에 사용할 직급 목록 조회
    List<HrSelectOptionDto> findPositions();

    // 권한 select 박스에 사용할 권한 목록 조회
    List<HrSelectOptionDto> findRoles();

    // 사원 등록 처리
    void createEmployee(EmployeeCreateRequestDto requestDto);

    // 사원 상세 정보 조회
    EmployeeDetailDto findEmployeeDetail(String empNo);

    // 이름을 기준으로 로그인 아이디 자동 생성
    String generateEmpId(String name);

    // 입사일을 기준으로 사번 자동 생성
    String generateEmpNo(String hireDate);

    // 사원 수정 화면에 보여줄 기존 사원 정보 조회
    EmployeeUpdateRequestDto findEmployeeForUpdate(String empNo);

    // 사원 수정 처리
    void updateEmployee(String empNo, EmployeeUpdateRequestDto requestDto);

    /*
     * 관리자 통계 데이터 조회
     */
    EmployeeStatisticsDto getEmployeeStatistics();

}