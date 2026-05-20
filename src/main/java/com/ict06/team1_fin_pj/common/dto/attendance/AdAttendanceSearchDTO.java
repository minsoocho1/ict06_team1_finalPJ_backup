package com.ict06.team1_fin_pj.common.dto.attendance;

import lombok.Getter;
import lombok.Setter;

/**
 * 관리자 근태 검색 조건 DTO
 *
 * 역할:
 * - 관리자 근태 현황 화면의 검색 조건을 담는 객체
 * - 근무일, 부서, 상태, 검색어를 Service / QueryDSL로 넘길 때 사용
 */
@Getter
@Setter
public class AdAttendanceSearchDTO {

    // 검색 조건: 근무일
    private String workDate;

    // 검색 조건: 부서 ID
    private String deptId;

    // 검색 조건: 근태 상태
    private String status;

    // 검색 조건: 사원명 검색어
    private String keyword;

    // 검색 조건: 정렬 기준
    private String sortType;

}