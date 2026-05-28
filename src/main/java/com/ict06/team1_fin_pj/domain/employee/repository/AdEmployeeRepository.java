package com.ict06.team1_fin_pj.domain.employee.repository;

import com.ict06.team1_fin_pj.common.dto.employee.EmployeeDetailDto;
import com.ict06.team1_fin_pj.common.dto.employee.EmployeeListDto;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/*
 * 사원 DB 접근 Repository
 *
 * JpaRepository<EmpEntity, String>
 * - EmpEntity: EMPLOYEE 테이블과 연결된 엔티티
 * - String: 기본키 empNo의 타입
 *
 * 이 Repository는 사원 목록 조회, 상세 조회, 중복 체크,
 * 사번/아이디 자동 생성을 위한 조회를 담당한다.
 */
public interface AdEmployeeRepository extends JpaRepository<EmpEntity, String> {

    /*
     * 사원 목록 검색 쿼리
     *
     * EmployeeListDto로 바로 조회한다.
     *
     * 검색 가능 조건:
     * - keyword: 사번, 아이디, 이름, 이메일, 연락처
     * - parentDeptId: 본부
     * - deptId: 팀
     * - positionId: 직급
     * - minPositionId: 최소 직급 (예: '주임'보다 높은 직급 보유 사원)
     * - roleId: 권한
     * - status: 상태
     *
     * 부서 검색 기준:
     * 1. 팀 선택 시
     *    - 해당 팀 소속 사원만 조회
     *
     * 2. 본부만 선택 시
     *    - 해당 본부 아래 모든 팀 소속 사원 조회
     *    - 혹시 본부에 직접 소속된 사원이 있으면 그 사원도 포함
     *
     * 3. 본부/팀 모두 미선택 시
     *    - 전체 부서 조회
     */
    @Query("""
    select new com.ict06.team1_fin_pj.common.dto.employee.EmployeeListDto(
        e.empNo,
        e.empId,
        e.name,
        e.email,
        e.phone,
        d.deptName,
        p.positionName,
        p.positionId,
        r.roleName,
        e.bank,
        e.accountNo,
        e.status,
        e.hireDate
    )
    from EmpEntity e
    join e.department d
    join e.position p
    join e.role r
    where e.isDeleted = 'N'
      and (
            :keyword is null
            or :keyword = ''
            or e.empNo like concat('%', :keyword, '%')
            or e.empId like concat('%', :keyword, '%')
            or e.name like concat('%', :keyword, '%')
            or e.email like concat('%', :keyword, '%')
            or e.phone like concat('%', :keyword, '%')
      )
      and (
            (:deptId is not null and d.deptId = :deptId)
            or
            (:deptId is null and (
                :parentDeptId is null
                or d.parentDept.deptId = :parentDeptId
                or d.deptId = :parentDeptId
            ))
      )
      and (:positionId is null or p.positionId = :positionId)
      
      and (:minPositionId is null or p.positionId > :minPositionId)
      
      and (:roleId is null or r.roleId = :roleId)
      and (
            :status is null
            or :status = ''
            or :status = '전체'
            or (:status = '기본' and e.status <> '퇴사')
            or (:status <> '기본' and e.status = :status)
      )
    order by e.hireDate desc, e.empNo desc
""")
    Page<EmployeeListDto> searchEmployees(
            String keyword,
            Integer parentDeptId,
            Integer deptId,
            Integer positionId,
            Integer minPositionId, // 최소 직급 (예: '주임'보다 높은 직급 보유 사원)
            Integer roleId,
            String status,
            Pageable pageable
    );

    /*
     * 사원 상세 조회 쿼리
     *
     * empNo로 특정 사원을 조회한다.
     * 상세 화면에서 필요한 정보를 EmployeeDetailDto로 바로 반환한다.
     */
    @Query("""
        select new com.ict06.team1_fin_pj.common.dto.employee.EmployeeDetailDto(
            e.empNo,
            e.empId,
            e.name,
            e.email,
            e.phone,
            d.deptId,
            d.deptName,
            p.positionId,
            p.positionName,
            r.roleId,
            r.roleName,
            e.bank,
            e.accountNo,
            e.status,
            e.hireDate,
            e.profileImg,
            e.signImg
        )
        from EmpEntity e
        join e.department d
        join e.position p
        join e.role r
        where e.empNo = :empNo
          and e.isDeleted = 'N'
    """)
    Optional<EmployeeDetailDto> findEmployeeDetail(String empNo);

    /*
     * 특정 연도로 시작하는 사번 목록 조회
     *
     * 예:
     * yearPrefix = "2026"
     * 조회 결과 = 20260001, 20260002 ...
     *
     * 사번 자동 생성할 때 사용한다.
     */
    @Query("""
        select e.empNo
        from EmpEntity e
        where e.empNo like concat(:yearPrefix, '%')
    """)
    List<String> findEmpNosByYearPrefix(String yearPrefix);

    /*
     * 특정 이름으로 시작하는 아이디 목록 조회
     *
     * 예:
     * name = "홍길동"
     * 조회 결과 = 홍길동01, 홍길동02 ...
     *
     * 로그인 아이디 자동 생성할 때 사용한다.
     */
    @Query("""
        select e.empId
        from EmpEntity e
        where e.empId like concat(:name, '%')
    """)
    List<String> findEmpIdsByNamePrefix(String name);

    // 사번 중복 여부 확인
    boolean existsByEmpNo(String empNo);

    // 로그인 아이디 중복 여부 확인
    boolean existsByEmpId(String empId);

    // 이메일 중복 여부 확인
    boolean existsByEmail(String email);

    // 연락처 중복 여부 확인
    boolean existsByPhone(String phone);

    // 계좌번호 중복 여부 확인
    boolean existsByAccountNo(String accountNo);

    /*
     * 수정할 때 이메일 중복 확인
     *
     * 현재 수정 중인 사원의 empNo는 제외하고 검사한다.
     * 즉, 자기 자신의 기존 이메일은 중복으로 보지 않는다.
     */
    boolean existsByEmailAndEmpNoNot(String email, String empNo);

    // 수정할 때 연락처 중복 확인
    boolean existsByPhoneAndEmpNoNot(String phone, String empNo);

    // 수정할 때 계좌번호 중복 확인
    boolean existsByAccountNoAndEmpNoNot(String accountNo, String empNo);

    /*
     * 상태별 사원 수 조회
     */
    long countByStatusAndIsDeleted(String status, String isDeleted);

    /*
     * 권한별 사원 수 조회
     */
    long countByRole_RoleIdAndIsDeleted(Integer roleId, String isDeleted);

    /*
     * 부서별 인원 수 조회
     *
     * 반환 예:
     * [
     *   [개발팀, 10],
     *   [인사팀, 5]
     * ]
     */
    @Query("""
    select d.deptName, count(e)
    from EmpEntity e
    join e.department d
    where e.isDeleted = 'N'
    group by d.deptName
    order by count(e) desc
""")
    List<Object[]> countEmployeesByDepartment();

    /*
     * 직급별 인원 수 조회
     *
     * 반환 예:
     * [
     *   [사원, 10],
     *   [대리, 5]
     * ]
     */
    @Query("""
    select p.positionName, count(e)
    from EmpEntity e
    join e.position p
    where e.isDeleted = 'N'
    group by p.positionName, p.positionId
    order by p.positionId asc
""")
    List<Object[]> countEmployeesByPosition();


}