package com.ict06.team1_fin_pj.domain.attendance.repository;

import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceSearchDTO;
import com.ict06.team1_fin_pj.domain.attendance.entity.AttendanceEntity;
import com.ict06.team1_fin_pj.domain.attendance.entity.AttendanceStatus;
import com.ict06.team1_fin_pj.domain.attendance.entity.QAttendanceEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.QDepartmentEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.QEmpEntity;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AttendanceRepositoryCustom 구현체
 *
 * 역할:
 * - 관리자 근태 현황 검색 조건을 실제 DB 조회에 적용한다.
 * - QueryDSL로 날짜 / 부서 / 상태 / 사원명 검색을 처리한다.
 * - Pageable로 페이징을 처리한다.
 * - 조회된 AttendanceEntity를 AdAttendanceDTO로 변환한다.
 */
@Repository
@RequiredArgsConstructor
public class AttendanceRepositoryImpl implements AttendanceRepositoryCustom {

    /**
     * QueryDSL 쿼리 작성 객체
     */
    private final JPAQueryFactory queryFactory;

    /**
     * 시간 출력 포맷
     *
     * 예:
     * 2026-05-12 09:03
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 관리자 근태 검색
     */
    @Override
    public Page<AdAttendanceDTO> searchAdminAttendance(
            AdAttendanceSearchDTO searchDTO,
            Pageable pageable
    ) {
        QAttendanceEntity attendance = QAttendanceEntity.attendanceEntity;
        QEmpEntity employee = QEmpEntity.empEntity;
        QDepartmentEntity department = QDepartmentEntity.departmentEntity;

        /**
         * 검색 조건을 담는 객체
         *
         * 조건이 있으면 and()로 추가하고,
         * 조건이 없으면 추가하지 않는다.
         */
        BooleanBuilder builder = new BooleanBuilder();

        /**
         * 1. 근무일 검색 조건
         *
         * 화면에서 넘어오는 값:
         * "2026-05-12"
         *
         * DB의 workDate는 LocalDate이므로 LocalDate로 변환해서 비교한다.
         */
        if (searchDTO.getWorkDate() != null && !searchDTO.getWorkDate().isBlank()) {
            LocalDate workDate = LocalDate.parse(searchDTO.getWorkDate());
            builder.and(attendance.workDate.eq(workDate));
        }

        /**
         * 2. 부서 검색 조건
         *
         * 화면에서 넘어오는 값:
         * "1", "2", "3"
         *
         * department.deptId가 Integer라면 Integer로 변환한다.
         */
        if (searchDTO.getDeptId() != null && !searchDTO.getDeptId().isBlank()) {
            Integer deptId = Integer.parseInt(searchDTO.getDeptId());
            builder.and(department.deptId.eq(deptId));
        }

        /**
         * 3. 근태 상태 검색 조건
         *
         * 화면에서 넘어오는 값:
         * ON_TIME, LATE, EARLY, ABSENT, LEFT
         *
         * AttendanceEntity.status가 Enum 타입이므로
         * AttendanceStatus.valueOf()로 변환해서 비교한다.
         */
        if (searchDTO.getStatus() != null && !searchDTO.getStatus().isBlank()) {
            AttendanceStatus status = AttendanceStatus.valueOf(searchDTO.getStatus());
            builder.and(attendance.status.eq(status));
        }

        /**
         * 4. 사원명 검색 조건
         *
         * 화면에서 넘어오는 값:
         * 김민수
         *
         * containsIgnoreCase:
         * - 대소문자 구분 없이 포함 검색
         */
        if (searchDTO.getKeyword() != null && !searchDTO.getKeyword().isBlank()) {
            builder.and(employee.name.containsIgnoreCase(searchDTO.getKeyword()));
        }

        /**
         * 실제 목록 조회
         *
         * select(attendance):
         * - 일단 AttendanceEntity를 조회한다.
         *
         * join(attendance.employee, employee):
         * - 근태와 사원을 조인한다.
         *
         * leftJoin(employee.department, department):
         * - 사원과 부서를 조인한다.
         *
         * offset / limit:
         * - Pageable 페이징 적용
         */
        List<AttendanceEntity> attendanceEntityList = queryFactory
                .select(attendance)
                .from(attendance)
                .join(attendance.employee, employee)
                .leftJoin(employee.department, department)
                .where(builder)
                .orderBy(getOrderSpecifier(searchDTO, attendance, employee))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        /**
         * 전체 개수 조회
         *
         * 페이징에서 totalPages / totalElements 계산에 사용된다.
         */
        Long total = queryFactory
                .select(attendance.count())
                .from(attendance)
                .join(attendance.employee, employee)
                .leftJoin(employee.department, department)
                .where(builder)
                .fetchOne();

        /**
         * Entity List를 DTO List로 변환
         *
         * Thymeleaf 화면에서는 AdAttendanceDTO만 사용한다.
         */
        List<AdAttendanceDTO> dtoList = attendanceEntityList.stream()
                .map(this::toAdAttendanceDTO)
                .toList();

        /**
         * PageImpl로 Page 객체 생성
         *
         * content: 현재 페이지 데이터
         * pageable: 현재 페이지 정보
         * total: 검색 조건에 맞는 전체 데이터 개수
         */
        return new PageImpl<>(dtoList, pageable, total == null ? 0 : total);
    }

    /**
     * AttendanceEntity를 관리자 화면용 DTO로 변환
     */
    private AdAttendanceDTO toAdAttendanceDTO(AttendanceEntity attendance) {

        return AdAttendanceDTO.builder()
                // 근태 ID
                // 수정 버튼 클릭 시 어떤 근태 기록을 수정할지 구분하는 값
                .attendanceId(attendance.getAttendanceId())
                .empNo(attendance.getEmployee().getEmpNo())
                .empName(attendance.getEmployee().getName())
                .deptName(
                        attendance.getEmployee().getDepartment() != null
                                ? attendance.getEmployee().getDepartment().getDeptName()
                                : "-"
                )
                .workDate(
                        attendance.getWorkDate() != null
                                ? attendance.getWorkDate().toString()
                                : "-"
                )
                .checkIn(
                        attendance.getCheckInAt() != null
                                ? attendance.getCheckInAt().format(DATE_TIME_FORMATTER)
                                : "-"
                )
                .checkOut(
                        attendance.getCheckOutAt() != null
                                ? attendance.getCheckOutAt().format(DATE_TIME_FORMATTER)
                                : "-"
                )
                .workHours(
                        attendance.getWorkHours() != null
                                ? attendance.getWorkHours().toString()
                                : "-"
                )
                .status(convertStatusToKorean(attendance.getStatus()))
                .build();
    }

    /**
     * 근태 상태 Enum을 화면 표시용 한글로 변환
     *
     * DB 값:
     * ON_TIME, LATE, EARLY, ABSENT, LEFT
     *
     * 화면 표시:
     * 정상, 지각, 조퇴, 결근, 퇴근완료
     */
    private String convertStatusToKorean(AttendanceStatus status) {

        if (status == null) {
            return "-";
        }

        return switch (status) {
            case ON_TIME -> "정상";
            case LATE -> "지각";
            case EARLY -> "조퇴";
            case ABSENT -> "결근";
            case LEFT -> "퇴근완료";
            case OVERTIME -> "연장근무";
            case LEAVE -> "휴가";
            case HALF_LEAVE -> "반차";
        };
    }

    /**
     * 정렬 조건 생성 메서드
     *
     * 화면에서 선택한 sortType 값에 따라
     * QueryDSL orderBy 조건을 다르게 적용한다.
     *
     * sortType 값:
     * - latest : 최신 근무일순
     * - oldest : 오래된 근무일순
     * - empNo  : 사번순
     * - name   : 이름순
     */
    private OrderSpecifier<?> getOrderSpecifier(
            AdAttendanceSearchDTO searchDTO,
            QAttendanceEntity attendance,
            QEmpEntity employee
    ) {
        /*
         * sortType이 null이거나 빈 값이면 기본 정렬을 적용한다.
         * 기본 정렬: 최신 근무일순
         */
        String sortType = searchDTO.getSortType();

        if (sortType == null || sortType.isBlank()) {
            return attendance.workDate.desc();
        }

        /*
         * 화면에서 넘어온 정렬값에 따라 orderBy 조건 반환
         */
        return switch (sortType) {

            // 오래된 근무일순
            case "oldest" -> attendance.workDate.asc();

            // 사번순
            case "empNo" -> employee.empNo.asc();

            // 이름순
            case "name" -> employee.name.asc();

            // 기본값: 최신 근무일순
            case "latest" -> attendance.workDate.desc();

            // 예상하지 못한 값이 들어오면 최신순으로 처리
            default -> attendance.workDate.desc();
        };
    }
}
