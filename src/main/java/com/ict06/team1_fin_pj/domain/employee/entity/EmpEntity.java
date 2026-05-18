/**
 * @FileName : EmpEntity.java
 * @Description : 사원 엔티티
 * @Author : 김다솜
 * @Date : 2026. 04. 22
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.12    김다솜        마이페이지 부서 표시를 위한 deptId/deptName JSON 응답 필드 추가
 */

package com.ict06.team1_fin_pj.domain.employee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ict06.team1_fin_pj.common.dto.BaseTimeEntity;
import com.ict06.team1_fin_pj.domain.payroll.entity.GradeCodeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/*
 * 사원 엔티티
 *
 * EMPLOYEE 테이블과 연결되는 클래스이다.
 * DB의 사원 정보를 Java 객체로 다루기 위해 사용한다.
 */
@Entity
@Table(name = "EMPLOYEE")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpEntity extends BaseTimeEntity {

    /*
     * 사번
     *
     * EMPLOYEE 테이블의 기본키이다.
     * 예: 20260001
     */
    @Id
    @Column(name = "emp_no", unique = true, nullable = false, length = 20)
    private String empNo;

    /*
     * 로그인 아이디
     *
     * 사원이 로그인할 때 사용하는 아이디이다.
     * 중복될 수 없다.
     */
    @Column(name = "emp_id", nullable = false, unique = true, length = 20)
    private String empId;

    /*
     * 비밀번호
     *
     * WRITE_ONLY 설정으로 JSON 응답에 비밀번호가 노출되지 않도록 한다.
     * 실제 저장 시에는 암호화된 비밀번호가 들어간다.
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    // 사원 이름
    @Column(nullable = false, length = 50)
    private String name;

    // 이메일
    // 중복될 수 없지만 nullable = true라서 비어 있을 수도 있다.
    @Column(unique = true, nullable = true)
    private String email;

    // 연락처
    // 중복될 수 없지만 nullable = true라서 비어 있을 수도 있다.
    @Column(unique = true, nullable = true)
    private String phone;

    // 은행명
    @Column(nullable = false, length = 20)
    private String bank;

    // 계좌번호
    @Column(name = "account_no", nullable = false, unique = true, length = 30)
    private String accountNo;

    /*
     * 부서
     *
     * 사원은 하나의 부서에 소속된다.
     * ManyToOne = 여러 사원이 하나의 부서에 속할 수 있다는 뜻이다.
     *
     * FetchType.LAZY는 실제로 department가 필요할 때 조회하겠다는 의미이다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id", nullable = false)
    @JsonIgnore
    private DepartmentEntity department;

    /*
     * 직급
     *
     * 사원은 하나의 직급을 가진다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    private PositionEntity position;

    /*
     * 권한
     *
     * 관리자, 일반 사용자 같은 권한 정보를 연결한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    /*
     * 급여 등급
     *
     * 급여 관리 쪽 GradeCodeEntity와 연결된다.
     * nullable이므로 아직 등급이 없을 수도 있다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id")
    private GradeCodeEntity grade;

    /*
     * 재직 상태
     *
     * 기본값은 "재직"이다.
     * 예: 재직, 휴직, 퇴사
     */
    @Builder.Default
    private String status = "재직";

    // 입사일
    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    // 퇴사일
    // 퇴사 상태가 되었을 때 날짜가 들어간다.
    @Column(name = "resignation_date")
    private LocalDate resignationDate;

    // 프로필 이미지 경로
    @Column(name = "profile_img")
    private String profileImg;

    // 서명 이미지 경로
    @Column(name = "sign_img")
    private String signImg;

    /*
     * 삭제 여부
     *
     * 실제 DB에서 데이터를 삭제하지 않고,
     * isDeleted 값을 Y/N으로 관리하는 방식이다.
     *
     * N = 삭제 안 됨
     * Y = 삭제됨
     */
    @Builder.Default
    @Column(name = "is_deleted", columnDefinition = "char(1)")
    private String isDeleted = "N";

    //RefreshToken 적용 위해 필드 추가
    @Column(name = "refresh_token")
    private String refreshToken;

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /*
     * 사원 기본 정보 수정 메서드
     *
     * 수정 화면에서 변경 가능한 기본 정보를 한 번에 변경한다.
     * 사번, 로그인 아이디, 입사일은 여기서 수정하지 않는다.
     */
    public void updateEmployeeInfo(
            String name,
            String email,
            String phone,
            DepartmentEntity department,
            PositionEntity position,
            RoleEntity role,
            String bank,
            String accountNo
    ) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.position = position;
        this.role = role;
        this.bank = bank;
        this.accountNo = accountNo;
    }

    // 재직 상태 변경
    public void changeStatus(String status) {
        this.status = status;
    }

    // 퇴사일 변경
    public void changeResignationDate(LocalDate resignationDate) {
        this.resignationDate = resignationDate;
    }

    // 비밀번호 변경
    public void changePassword(String password) {
        this.password = password;
    }

    // 프로필 이미지 경로 변경
    public void changeProfileImg(String profileImg) {
        this.profileImg = profileImg;
    }

    // 서명 이미지 경로 변경
    public void changeSignImg(String signImg) {
        this.signImg = signImg;
    }

    @JsonProperty("deptId")
    public Integer getDeptId() {
        return department != null ? department.getDeptId() : null;
    }

    @JsonProperty("deptName")
    public String getDeptName() {
        return department != null ? department.getDeptName() : null;
    }
}
