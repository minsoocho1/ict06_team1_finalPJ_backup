package com.ict06.team1_fin_pj.domain.attendance.service;

import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceSummaryDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceSearchDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceStatisticsDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdDepartmentWorkHourDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdMonthlyAttendanceTrendDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdDepartmentLateRateDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdTodayAttendanceStatusDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceUpdateRequestDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceChangeLogDTO;

import com.ict06.team1_fin_pj.domain.attendance.entity.AttChangeLogEntity;
import com.ict06.team1_fin_pj.domain.attendance.repository.AttChangeLogRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.attendance.entity.AttendanceEntity;
import com.ict06.team1_fin_pj.domain.employee.repository.EmployeeRepository;
import com.ict06.team1_fin_pj.domain.attendance.repository.AttendanceRepository;
import com.ict06.team1_fin_pj.domain.attendance.entity.AttendanceStatus;
import com.ict06.team1_fin_pj.domain.attendance.excel.AttendanceExcelExporter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 관리자 근태 관리 Service 구현체
@Service
public class AdAttendanceServiceImpl implements AdAttendanceService {

    // 근태 Repository
    private final AttendanceRepository attendanceRepository;

    // 사원 Repository
    private final EmployeeRepository employeeRepository;

    // 근태 수정 이력 Repository
    private final AttChangeLogRepository attChangeLogRepository;

    // 날짜/시간 출력 포맷
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 생성자 주입
    public AdAttendanceServiceImpl(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            AttChangeLogRepository attChangeLogRepository
    ) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.attChangeLogRepository = attChangeLogRepository;
    }

    // 관리자 근태 목록 조회
    @Override
    public Page<AdAttendanceDTO> getAttendanceList(
            AdAttendanceSearchDTO searchDTO,
            int page,
            int size
    ) {
        // 1. 페이지 번호 보정
        // Spring Data JPA의 Page 번호는 0부터 시작한다.
        if (page < 0) {
            page = 0;
        }

        // 2. 한 페이지 크기 보정
        // size는 한 페이지에 보여줄 데이터 개수이다.
        if (size < 1) {
            size = 10;
        }

        // 3. Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        /*
         * 4. QueryDSL 관리자 검색 메서드 호출
         *
         * attendanceRepository.searchAdminAttendance(searchDTO, pageable)
         * → 검색 조건 + 페이징을 같이 적용
         *
         * 검색 조건:
         * - workDate: 근무일
         * - deptId: 부서
         * - status: 근태 상태
         * - keyword: 사원명
         */
        return attendanceRepository.searchAdminAttendance(searchDTO, pageable);
    }

    /**
     * 관리자 근태 현황 Excel 다운로드
     *
     * 처리 흐름:
     * 1. 화면에서 전달된 검색 조건을 그대로 사용한다.
     * 2. Excel 다운로드는 페이징 없이 전체 검색 결과를 내려받는다.
     * 3. 기존 관리자 근태 조회 메서드를 큰 size로 호출한다.
     * 4. 조회된 목록을 AttendanceExcelExporter에 전달해 Excel byte 배열을 생성한다.
     */
    @Override
    public byte[] downloadAttendanceExcel(AdAttendanceSearchDTO searchDTO) {

        // Excel 다운로드는 현재 검색 조건에 해당하는 전체 데이터를 대상으로 한다.
        // 발표/시연 기준으로 충분히 큰 size를 사용한다.
        int excelPage = 0;
        int excelSize = 10000;

        // 기존 관리자 근태 조회 로직 재사용
        // 검색, 부서, 상태, 정렬 조건이 그대로 적용된다.
        Page<AdAttendanceDTO> attendancePage =
                getAttendanceList(
                        searchDTO,
                        excelPage,
                        excelSize
                );

        // 현재 검색 조건에 해당하는 전체 근태 목록
        List<AdAttendanceDTO> attendanceList =
                attendancePage.getContent();

        // Excel 생성 전용 클래스
        AttendanceExcelExporter exporter =
                new AttendanceExcelExporter();

        // Excel 파일 byte 배열 반환
        return exporter.export(attendanceList);
    }

    // 관리자 근태 상단 요약 카드 조회
    @Override
    public AdAttendanceSummaryDTO getAttendanceSummary() {

        // 전체 근태 건수
        // ATTENDANCE 테이블 전체 row 수 조회
        int totalCount = (int) attendanceRepository.count();

        // 정상 출근 수
        // status = ON_TIME 인 데이터 개수 조회
        int onTimeCount = (int) attendanceRepository.countByStatus(AttendanceStatus.ON_TIME);

        // 지각 수
        // status = LATE 인 데이터 개수 조회
        int lateCount = (int) attendanceRepository.countByStatus(AttendanceStatus.LATE);

        // 조퇴 수
        // status = EARLY 인 데이터 개수 조회
        int earlyCount = (int) attendanceRepository.countByStatus(AttendanceStatus.EARLY);

        // 화면 상단 카드에 전달할 DTO 생성
        return AdAttendanceSummaryDTO.builder()
                .totalCount(totalCount)
                .onTimeCount(onTimeCount)
                .lateCount(lateCount)
                .earlyCount(earlyCount)
                .build();
    }

    /**
     * 관리자 근태 통계 조회
     *
     * 계산 항목:
     * - 전체 평균 근무시간
     * - 전체 지각률
     * - 전체 조퇴율
     * - 전체 정상 출근률
     */
    @Override
    public AdAttendanceStatisticsDTO getAttendanceStatistics() {

        // 1. 전체 근태 데이터 조회
        List<AttendanceEntity> attendanceList = attendanceRepository.findAll();

        // 2. 전체 근태 건수
        int totalCount = attendanceList.size();

        // 3. 데이터가 하나도 없을 경우
        if (totalCount == 0) {
            return AdAttendanceStatisticsDTO.builder()
                    // 평균/비율 값
                    .averageWorkHours(0.0)
                    .lateRate(0.0)
                    .earlyRate(0.0)
                    .onTimeRate(0.0)

                    // 상태별 건수 값
                    .totalCount(0)
                    .onTimeCount(0)
                    .lateCount(0)
                    .earlyCount(0)
                    .build();
        }

        //4. 전체 평균 근무시간 계산
        double averageWorkHours = attendanceList.stream()
                .filter(attendance -> attendance.getWorkHours() != null)
                .map(AttendanceEntity::getWorkHours)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        // 5. 지각 건수 계산
        long lateCount = attendanceList.stream()
                .filter(attendance -> attendance.getStatus() == AttendanceStatus.LATE)
                .count();

        // 6. 조퇴 건수 계산
        long earlyCount = attendanceList.stream()
                .filter(attendance -> attendance.getStatus() == AttendanceStatus.EARLY)
                .count();

        // 7. 정상 출근 건수 계산
        long onTimeCount = attendanceList.stream()
                .filter(attendance -> attendance.getStatus() == AttendanceStatus.ON_TIME)
                .count();

        // 8. 비율 계산
        double lateRate = (double) lateCount / totalCount * 100;
        double earlyRate = (double) earlyCount / totalCount * 100;
        double onTimeRate = (double) onTimeCount / totalCount * 100;

        // 9. 소수점 첫째 자리까지 반올림
        averageWorkHours = roundOneDecimal(averageWorkHours);
        lateRate = roundOneDecimal(lateRate);
        earlyRate = roundOneDecimal(earlyRate);
        onTimeRate = roundOneDecimal(onTimeRate);

        // 10. 계산된 통계 값을 DTO에 담아서 반환
        return AdAttendanceStatisticsDTO.builder()
                // 평균/비율 값
                .averageWorkHours(averageWorkHours)
                .lateRate(lateRate)
                .earlyRate(earlyRate)
                .onTimeRate(onTimeRate)

                // 상태별 건수 값
                .totalCount(totalCount)
                .onTimeCount((int) onTimeCount)
                .lateCount((int) lateCount)
                .earlyCount((int) earlyCount)
                .build();
    }

    /**
     * 부서별 평균 근무시간 조회
     *
     * 계산 기준:
     * - ATTENDANCE → EMPLOYEE → DEPARTMENT 연결 기준
     * - 부서명별로 그룹화
     * - workHours 평균 계산
     *
     * 사용 위치:
     * - 관리자 근태 통계 화면
     * - 부서별 평균 근무시간 차트
     */
    @Override
    public List<AdDepartmentWorkHourDTO> getDepartmentAverageWorkHours() {


        // 1. 전체 근태 데이터 조회
        List<AttendanceEntity> attendanceList = attendanceRepository.findAll();

        // 2. 부서별로 그룹화
        Map<String, List<AttendanceEntity>> groupedByDepartment =
                attendanceList.stream()
                        .collect(Collectors.groupingBy(attendance -> {
                            if (attendance.getEmployee() == null ||
                                    attendance.getEmployee().getDepartment() == null) {
                                return "부서 없음";
                            }

                            return attendance.getEmployee()
                                    .getDepartment()
                                    .getDeptName();
                        }));


        // 3. 부서별 평균 근무시간 계산 후 DTO 변환
        return groupedByDepartment.entrySet()
                .stream()
                .map(entry -> {

                    // 현재 그룹의 부서명
                    String deptName = entry.getKey();

                    // 현재 부서에 해당하는 근태 목록
                    List<AttendanceEntity> deptAttendanceList = entry.getValue();

                    // 현재 부서의 평균 근무시간 계산
                    // workHours가 null인 데이터는 제외
                    double averageWorkHours = deptAttendanceList.stream()
                            .filter(attendance -> attendance.getWorkHours() != null)
                            .map(AttendanceEntity::getWorkHours)
                            .mapToDouble(BigDecimal::doubleValue)
                            .average()
                            .orElse(0.0);

                    // 소수점 첫째 자리 반올림
                    averageWorkHours = roundOneDecimal(averageWorkHours);

                    //차트용 DTO 생성
                    return AdDepartmentWorkHourDTO.builder()
                            .deptName(deptName)
                            .averageWorkHours(averageWorkHours)
                            .build();
                })
                .toList();
    }

    /**
     * 월별 근태 추이 조회
     *
     * 계산 기준:
     * - ATTENDANCE.workDate 기준으로 월을 구한다.
     * - 같은 월끼리 그룹화한다.
     * - 각 월마다 정상 / 지각 / 조퇴 건수를 계산한다.
     *
     * 사용 위치:
     * - 관리자 근태 통계 화면
     * - 월별 근태 추이 차트
     */
    @Override
    public List<AdMonthlyAttendanceTrendDTO> getMonthlyAttendanceTrend() {

        // 1. 전체 근태 데이터 조회
        List<AttendanceEntity> attendanceList = attendanceRepository.findAll();

        // 2. 월 포맷 지정
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // 3. workDate 기준 월별 그룹화
        // workDate가 null인 데이터는 통계에서 제외한다.
        Map<String, List<AttendanceEntity>> groupedByMonth =
                attendanceList.stream()
                        .filter(attendance -> attendance.getWorkDate() != null)
                        .collect(Collectors.groupingBy(
                                attendance -> attendance.getWorkDate().format(monthFormatter)
                        ));

        // 4. 월별 그룹 데이터를 DTO로 변환
        return groupedByMonth.entrySet()
                .stream()
                .map(entry -> {

                    // 현재 월
                    String month = entry.getKey();

                    // 현재 월에 해당하는 근태 목록
                    List<AttendanceEntity> monthlyList = entry.getValue();

                    // 정상 출근 건수
                    long onTimeCount = monthlyList.stream()
                            .filter(attendance -> attendance.getStatus() == AttendanceStatus.ON_TIME)
                            .count();

                    // 지각 건수
                    long lateCount = monthlyList.stream()
                            .filter(attendance -> attendance.getStatus() == AttendanceStatus.LATE)
                            .count();

                    // 조퇴 건수
                    long earlyCount = monthlyList.stream()
                            .filter(attendance -> attendance.getStatus() == AttendanceStatus.EARLY)
                            .count();


                    // 월별 근태 추이 DTO 생성
                    return AdMonthlyAttendanceTrendDTO.builder()
                            .month(month)
                            .onTimeCount(onTimeCount)
                            .lateCount(lateCount)
                            .earlyCount(earlyCount)
                            .build();
                })

                // 월 오름차순 정렬
                .sorted(Comparator.comparing(AdMonthlyAttendanceTrendDTO::getMonth))
                .toList();
    }

    /**
     * 부서별 지각률 조회
     *
     * 계산 기준:
     * - 부서별 전체 근태 건수 계산
     * - 부서별 지각 건수 계산
     * - 지각률 = (지각 건수 / 전체 건수) * 100
     *
     * 사용 위치:
     * - 관리자 근태 통계 화면
     * - 부서별 지각률 차트
     */
    @Override
    public List<AdDepartmentLateRateDTO> getDepartmentLateRates() {

        // 1. 전체 근태 데이터 조회
        List<AttendanceEntity> attendanceList =
                attendanceRepository.findAll();

        // 2. 부서별 그룹화
        Map<String, List<AttendanceEntity>> groupedByDepartment =
                attendanceList.stream()
                        .collect(Collectors.groupingBy(attendance -> {

                            if (attendance.getEmployee() == null ||
                                    attendance.getEmployee().getDepartment() == null) {

                                return "부서 없음";
                            }

                            return attendance.getEmployee()
                                    .getDepartment()
                                    .getDeptName();
                        }));

        // 3. 부서별 지각률 계산
        return groupedByDepartment.entrySet()
                .stream()
                .map(entry -> {

                    // 현재 부서명
                    String deptName = entry.getKey();

                    // 현재 부서 근태 목록
                    List<AttendanceEntity> deptAttendanceList =
                            entry.getValue();

                    // 부서 전체 건수
                    long totalCount =
                            deptAttendanceList.size();

                    // 부서 지각 건수
                    long lateCount = deptAttendanceList.stream()
                            .filter(attendance ->
                                    attendance.getStatus() == AttendanceStatus.LATE)
                            .count();

                    // 지각률 계산
                    double lateRate = 0.0;

                    if (totalCount > 0) {
                        lateRate =
                                ((double) lateCount / totalCount) * 100;
                    }

                    // 소수점 첫째 자리 반올림
                    lateRate = roundOneDecimal(lateRate);

                    // DTO 생성
                    return AdDepartmentLateRateDTO.builder()
                            .deptName(deptName)
                            .totalCount(totalCount)
                            .lateCount(lateCount)
                            .lateRate(lateRate)
                            .build();
                })

                // 지각률 높은 순 정렬
                .sorted((a, b) ->
                        Double.compare(b.getLateRate(), a.getLateRate()))

                .toList();
    }

    /**
     * 오늘 출근 현황 조회
     *
     * 계산 기준:
     * - 오늘 날짜 기준 근태 기록 조회
     * - checkInAt이 있으면 출근 완료로 계산
     * - status가 LATE이면 오늘 지각으로 계산
     * - 미출근 = 전체 사원 수 - 오늘 출근 완료 건수
     */
    @Override
    public AdTodayAttendanceStatusDTO getTodayAttendanceStatus() {

        // 1. 오늘 날짜 구하기
        LocalDate today = LocalDate.now();

        // 2. 전체 사원 수 조회
        long totalEmployeeCount = employeeRepository.count();

        // 3. 오늘 날짜의 근태 기록 조회
        List<AttendanceEntity> todayAttendanceList =
                attendanceRepository.findByWorkDate(today);

        // 4. 오늘 출근 완료 건수
        long checkedInCount = todayAttendanceList.stream()
                .filter(attendance -> attendance.getCheckInAt() != null)
                .count();

        // 5. 오늘 지각 건수
        long lateCount = todayAttendanceList.stream()
                .filter(attendance -> attendance.getStatus() == AttendanceStatus.LATE)
                .count();

        // 6. 오늘 미출근 건수
        long notCheckedInCount = totalEmployeeCount - checkedInCount;

        if (notCheckedInCount < 0) {
            notCheckedInCount = 0;
        }

        // 7. DTO로 반환
        return AdTodayAttendanceStatusDTO.builder()
                .checkedInCount(checkedInCount)
                .lateCount(lateCount)
                .notCheckedInCount(notCheckedInCount)
                .build();
    }

    /**
     * 관리자 근태 수정
     *
     * 관리자 화면에서 출근 시간, 퇴근 시간, 근태 상태를 수정한다.
     * 수정 사유는 필수값으로 검증한다.
     */
    @Override
    @Transactional
    public void updateAttendanceByAdmin(
            AdAttendanceUpdateRequestDTO request,
            String adminEmpNo
    ) {
        // 1. 수정 사유 필수 검증
        if (request.getChangeReason() == null ||
                request.getChangeReason().trim().isEmpty()) {
            throw new IllegalArgumentException("수정 사유는 필수입니다.");
        }

        // 2. 수정 대상 근태 조회
        AttendanceEntity attendance = attendanceRepository.findById(request.getAttendanceId())
                .orElseThrow(() -> new IllegalArgumentException("근태 정보를 찾을 수 없습니다."));

        // 3. 수정 전 값 백업
        // 다음 단계에서 ATTENDANCE_CHANGE_LOG 저장할 때 사용할 값들
        LocalDateTime beforeCheckIn = attendance.getCheckInAt();
        LocalDateTime beforeCheckOut = attendance.getCheckOutAt();
        AttendanceStatus beforeStatus = attendance.getStatus();

        // 4. 관리자 수정 적용
        attendance.updateByAdmin(
                request.getCheckInAt(),
                request.getCheckOutAt(),
                request.getStatus()
        );

        // 5. 저장
        // @Transactional 상태에서는 Dirty Checking으로 자동 반영되지만,
        // 지금은 흐름을 명확히 보기 위해 save를 호출한다.
        attendanceRepository.save(attendance);

        // 6. 수정한 관리자 조회
        // adminEmpNo는 로그인한 관리자 사번이다.
        // EMPLOYEE 테이블에서 해당 관리자를 조회한다.
        EmpEntity admin = employeeRepository.findByEmpNo(adminEmpNo)
                .orElseThrow(() -> new IllegalArgumentException("관리자 정보를 찾을 수 없습니다."));

        // 7. 근태 수정 이력 생성
        // ATTENDANCE_CHANGE_LOG 테이블에 저장할 로그 객체를 만든다.
        AttChangeLogEntity changeLog = AttChangeLogEntity.builder()
                .attendance(attendance)                         // 수정 대상 근태 기록
                .workDate(attendance.getWorkDate())              // 근무 기준일
                .beforeCheckIn(beforeCheckIn)                    // 수정 전 출근 시간
                .afterCheckIn(request.getCheckInAt())            // 수정 후 출근 시간
                .beforeCheckOut(beforeCheckOut)                  // 수정 전 퇴근 시간
                .afterCheckOut(request.getCheckOutAt())          // 수정 후 퇴근 시간
                .beforeStatus(beforeStatus)                      // 수정 전 근태 상태
                .afterStatus(request.getStatus())                // 수정 후 근태 상태
                .changeReason(request.getChangeReason())         // 수정 사유
                .changedBy(admin)                                // 수정한 관리자
                .changedAt(LocalDateTime.now())                  // 수정 일시
                .build();

        // 8. 근태 수정 이력 저장
        attChangeLogRepository.save(changeLog);
    }

    /**
     * 관리자 근태 수정 이력 페이징 조회
     *
     * ATTENDANCE_CHANGE_LOG 테이블의 데이터를
     * 최신 수정일시 순으로 페이징 조회한 뒤,
     * 화면 출력용 DTO로 변환한다.
     */
    @Override
    public Page<AdAttendanceChangeLogDTO> getAttendanceChangeLogs(int page, int size) {

        // 1. 페이지 요청 객체 생성
        // page: 0부터 시작하는 페이지 번호
        // size: 한 페이지에 보여줄 개수
        Pageable pageable = PageRequest.of(page, size);

        // 2. Entity Page 조회 후 DTO Page로 변환
        return attChangeLogRepository.findAllByOrderByChangedAtDesc(pageable)
                .map(log -> AdAttendanceChangeLogDTO.builder()
                        .changeLogId(log.getChangeLogId())
                        .attendanceId(log.getAttendance().getAttendanceId())
                        .workDate(log.getWorkDate() != null ? log.getWorkDate().toString() : "-")
                        .beforeCheckIn(log.getBeforeCheckIn() != null ? log.getBeforeCheckIn().toLocalTime().toString() : "-")
                        .afterCheckIn(log.getAfterCheckIn() != null ? log.getAfterCheckIn().toLocalTime().toString() : "-")
                        .beforeCheckOut(log.getBeforeCheckOut() != null ? log.getBeforeCheckOut().toLocalTime().toString() : "-")
                        .afterCheckOut(log.getAfterCheckOut() != null ? log.getAfterCheckOut().toLocalTime().toString() : "-")
                        .beforeStatus(convertStatusToKorean(log.getBeforeStatus()))
                        .afterStatus(convertStatusToKorean(log.getAfterStatus()))
                        .changeReason(log.getChangeReason())
                        .changedBy(log.getChangedBy() != null ? log.getChangedBy().getEmpNo() : "-")
                        .changedByName(log.getChangedBy() != null ? log.getChangedBy().getName() : "-")
                        .changedAt(log.getChangedAt() != null ? log.getChangedAt().format(DATE_TIME_FORMATTER) : "-")
                        .build());
    }

    // 근태 상태 Enum을 화면 출력용 한글로 변환
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

    // 소수점 첫째 자리 반올림 메서드
    private double roundOneDecimal(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

}
