package com.ict06.team1_fin_pj.common.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/*
 * 조직도 사원 목록 DTO
 *
 * 조직도에서 부서를 클릭했을 때
 * 오른쪽에 표시할 사원 정보이다.
 *
 * 관리자 Thymeleaf 화면과
 * 사용자 React 화면에서 공통으로 사용한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgEmployeeDto {

    /*
     * 사번
     */
    private String empNo;

    /*
     * 사원 아이디
     */
    private String empId;

    /*
     * 사원 이름
     */
    private String name;

    /*
     * 부서 ID
     */
    private Integer deptId;

    /*
     * 부서명
     */
    private String deptName;

    /*
     * 직급 ID
     *
     * 직급 정렬 시 사용할 수 있다.
     */
    private Integer positionId;

    /*
     * 직급명
     *
     * 예:
     * 수석, 선임, 책임, 사원
     */
    private String positionName;

    /*
     * 재직 상태
     *
     * 예:
     * 재직, 휴직, 퇴사
     */
    private String status;

    /*
     * 이메일
     */
    private String email;

    /*
     * 연락처
     */
    private String phone;

    /*
     * 프로필 이미지 경로
     */
    private String profileImg;

    /*
     * 입사일
     */
    private LocalDate hireDate;
}