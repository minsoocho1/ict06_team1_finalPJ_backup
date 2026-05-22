package com.ict06.team1_fin_pj.domain.attendance.service;

import com.ict06.team1_fin_pj.domain.attendance.entity.LeaveOccurrenceEntity;
import com.ict06.team1_fin_pj.domain.attendance.entity.LeaveRequestEntity;
import com.ict06.team1_fin_pj.domain.attendance.repository.LeaveOccurrenceRepository;
import com.ict06.team1_fin_pj.domain.attendance.repository.LeaveRequestRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.employee.repository.EmployeeRepository;
import com.ict06.team1_fin_pj.domain.attendance.repository.LeaveTypeRepository;
import com.ict06.team1_fin_pj.domain.attendance.entity.LeaveTypeEntity;
import com.ict06.team1_fin_pj.domain.attendance.excel.LeaveExcelExporter;

import com.ict06.team1_fin_pj.common.dto.attendance.LeaveHistoryDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.LeaveSummaryDTO;
import com.ict06.team1_fin_pj.common.dto.attendance.AdLeaveStatusDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


// 연차 현황 Service 구현체
@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    // 연차 발생/사용/잔여 조회 Repository
    private final LeaveOccurrenceRepository leaveOccurrenceRepository;

    // 연차 신청 내역 조회 Repository
    private final LeaveRequestRepository leaveRequestRepository;

    // 사원 조회 Repository
    private final EmployeeRepository employeeRepository;

    // 휴가 유형 Repository
    private final LeaveTypeRepository leaveTypeRepository;

    // ==============================
    // 1. 연차 요약 조회
    // ==============================
    // LEAVE_OCCURRENCE 테이블 조회
    @Override
    public LeaveSummaryDTO getLeaveSummary(String empNo) {

        // 현재 연도
        int currentYear = LocalDate.now().getYear();

        // 특정 사원의 현재 연도 연차 조회
        List<LeaveOccurrenceEntity> occurrences =
                leaveOccurrenceRepository
                        .findByEmployee_EmpNoAndTargetYear(
                                empNo,
                                currentYear
                        );

        // 총 연차 합계
        BigDecimal totalDays = occurrences.stream()
                .map(LeaveOccurrenceEntity::getOccurDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 사용 연차 합계
        BigDecimal usedDays = occurrences.stream()
                .map(occurrence ->
                        occurrence.getUsed_days() == null
                                ? BigDecimal.ZERO
                                : occurrence.getUsed_days()
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 잔여 연차 합계
        BigDecimal remainDays = occurrences.stream()
                .map(occurrence ->
                        occurrence.getRemain_days() == null
                                ? BigDecimal.ZERO
                                : occurrence.getRemain_days()
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new LeaveSummaryDTO(
                totalDays,
                usedDays,
                remainDays
        );
    }

    // ==============================
    // 2. 연차 사용 내역 조회
    // ==============================
    // LEAVE_REQUEST + LEAVE_TYPE 조회
    @Override
    public List<LeaveHistoryDTO> getLeaveHistory(String empNo) {

        // 특정 사원의 연차 신청 내역 조회
        List<LeaveRequestEntity> requests =
                leaveRequestRepository
                        .findByEmployee_EmpNoOrderByStartDateDesc(empNo);

        // Entity -> DTO 변환
        return requests.stream()
                .map(request -> new LeaveHistoryDTO(

                        // 시작일
                        request.getStartDate(),

                        // 종료일
                        request.getEndDate(),

                        // 휴가 종류명
                        request.getLeaveType().getTypeName(),

                        // 사용 일수
                        request.getLeaveDays(),

                        // 상태값
                        request.getStatus().name()
                ))
                .toList();
    }

    // ==============================
    // 3. 연차 자동 부여
    // ==============================
    // 규칙:
    // 1년 이상 근무 → 15일 부여
    // 1년 미만 근무 → 남은 개월 수만큼 부여
    @Override
    public LeaveSummaryDTO grantAnnualLeave(String empNo) {

        // 1. 사원 조회
        EmpEntity employee = employeeRepository.findByEmpNo(empNo)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사원입니다.")
                );

        // 2. 현재 날짜
        LocalDate today = LocalDate.now();

        // 현재 연도
        int currentYear = today.getYear();

        // 3. 올해 이미 연차가 부여됐는지 확인
        List<LeaveOccurrenceEntity> existingOccurrences =
                leaveOccurrenceRepository
                        .findByEmployee_EmpNoAndTargetYear(
                                empNo,
                                currentYear
                        );

        // 이미 부여된 경우 중복 INSERT 방지
        if (!existingOccurrences.isEmpty()) {
            return getLeaveSummary(empNo);
        }

        // 4. 연차 타입 조회
        LeaveTypeEntity annualLeaveType =
                leaveTypeRepository.findByTypeName("연차")
                        .orElseThrow(() ->
                                new IllegalArgumentException("연차 타입이 존재하지 않습니다.")
                        );

        // 5. 입사일 조회
        LocalDate hireDate = employee.getHireDate();

        // 6. 근속 기간 계산
        Period period = Period.between(hireDate, today);

        // 7. 부여 연차 계산
        BigDecimal grantDays;

        // DB에 저장할 연차 발생 사유
        String grantReason;

        // 입사 후 1년 이상
        if (period.getYears() >= 1) {

            // 1년 이상 근속자는 정기 연차 15일 부여
            grantDays = new BigDecimal("15.0");

            // 관리자 화면이나 DB에서 확인하기 쉬운 사유 문구
            grantReason = "1년 이상 근속자 정기 연차 15일 부여";

        } else {

            // 1년 미만자는 올해 남은 개월 수만큼 월차 성격으로 부여
            int remainingMonths =
                    12 - today.getMonthValue() + 1;

            grantDays =
                    BigDecimal.valueOf(remainingMonths);

            // 예: 5월 실행 시 5월~12월 = 8일
            grantReason =
                    "1년 미만자 월차 선부여: " + remainingMonths + "일";
        }

        // 8. 연차 발생 내역 저장
        LeaveOccurrenceEntity occurrence =
                LeaveOccurrenceEntity.builder()
                        .employee(employee)
                        .leaveType(annualLeaveType)
                        .targetYear(currentYear)
                        .occurDate(today)
                        .occurDays(grantDays)
                        .used_days(BigDecimal.ZERO)
                        .remain_days(grantDays)
                        .expiryDate(
                                LocalDate.of(currentYear, 12, 31)
                        )
                        .reason(grantReason)
                        .build();

        leaveOccurrenceRepository.save(occurrence);

        // 9. 저장 후 최신 연차 요약 반환
        return getLeaveSummary(empNo);
    }

    /**
     * 관리자용 전체 사원 연차 자동 부여
     * 1년 이상자 정기 연차용
     *
     * 처리 흐름:
     * 1. 전체 사원을 조회한다.
     * 2. 재직 중인 사원만 대상으로 한다.
     * 3. 올해 이미 연차가 부여된 사원은 건너뛴다.
     * 4. 기존 1인 연차 부여 메서드 grantAnnualLeave(empNo)를 재사용한다.
     * 5. 새로 연차가 부여된 사원 수를 반환한다.
     */
    @Override
    public int grantAnnualLeaveForAllEmployees() {

        // 전체 사원 조회
        List<EmpEntity> employees = employeeRepository.findAll();

        // 실제로 새 연차가 부여된 사원 수
        int grantedCount = 0;

        // 현재 연도
        int currentYear = LocalDate.now().getYear();

        // 전체 사원을 한 명씩 확인
        for (EmpEntity employee : employees) {

            // 삭제된 사원은 제외
            if ("Y".equals(employee.getIsDeleted())) {
                continue;
            }

            // 재직 상태가 아닌 사원은 제외
            if (!"재직".equals(employee.getStatus())) {
                continue;
            }

            // 입사일 기준 근속 기간 계산
            // 정기 연차 부여는 1년 이상 근속자만 대상으로 한다.
            Period period =
                    Period.between(
                            employee.getHireDate(),
                            LocalDate.now()
                    );

            // 1년 미만자는 정기 연차 부여 대상이 아니므로 제외
            // 1년 미만자의 월차는 별도 매월 Scheduler에서 처리
            if (period.getYears() < 1) {
                continue;
            }

            // 올해 이미 연차가 부여된 사원인지 확인
            List<LeaveOccurrenceEntity> existingOccurrences =
                    leaveOccurrenceRepository.findByEmployee_EmpNoAndTargetYear(
                            employee.getEmpNo(),
                            currentYear
                    );

            // 이미 있으면 중복 부여 방지
            if (!existingOccurrences.isEmpty()) {
                continue;
            }

            // 기존 1명용 연차 자동 부여 로직 재사용
            grantAnnualLeave(employee.getEmpNo());

            // 새로 부여된 인원 수 증가
            grantedCount++;
        }

        return grantedCount;
    }

    /**
     * 1년 미만 신입사원 월차 자동 부여
     *
     * 처리 흐름:
     * 1. 전체 사원을 조회한다.
     * 2. 재직 중인 사원만 대상으로 한다.
     * 3. 입사 1년 미만 사원만 대상으로 한다.
     * 4. 이번 달 이미 월차가 부여된 사원은 제외한다.
     * 5. 월차 1일을 LEAVE_OCCURRENCE에 저장한다.
     */
    @Override
    public int grantMonthlyLeaveForNewEmployees() {

        // 전체 사원 조회
        List<EmpEntity> employees = employeeRepository.findAll();

        // 현재 날짜
        LocalDate today = LocalDate.now();

        // 현재 연도
        int currentYear = today.getYear();

        // 새로 월차가 부여된 사원 수
        int grantedCount = 0;

        // 연차 타입 조회
        LeaveTypeEntity annualLeaveType =
                leaveTypeRepository.findByTypeName("연차")
                        .orElseThrow(() ->
                                new IllegalArgumentException("연차 타입이 존재하지 않습니다.")
                        );

        // 전체 사원을 한 명씩 확인
        for (EmpEntity employee : employees) {

            // 삭제된 사원 제외
            if ("Y".equals(employee.getIsDeleted())) {
                continue;
            }

            // 재직 상태가 아닌 사원 제외
            if (!"재직".equals(employee.getStatus())) {
                continue;
            }

            // 입사일 기준 근속 기간 계산
            Period period =
                    Period.between(
                            employee.getHireDate(),
                            today
                    );

            // 1년 이상자는 월차 대상이 아니므로 제외
            if (period.getYears() >= 1) {
                continue;
            }

            // 입사 당월은 제외
            // 예: 5월 입사자는 5월 1일 월차 부여 대상이 아님
            if (employee.getHireDate().getYear() == today.getYear()
                    && employee.getHireDate().getMonthValue() == today.getMonthValue()) {
                continue;
            }

            // 이번 달 이미 월차가 부여됐는지 확인
            // 같은 달에 여러 번 Scheduler가 실행되어도 중복 INSERT를 막기 위한 조건이다.
            List<LeaveOccurrenceEntity> existingOccurrences =
                    leaveOccurrenceRepository.findByEmployee_EmpNoAndTargetYear(
                            employee.getEmpNo(),
                            currentYear
                    );

            boolean alreadyGrantedThisMonth =
                    existingOccurrences.stream()
                            .anyMatch(occurrence ->
                                    occurrence.getOccurDate() != null
                                            && occurrence.getOccurDate().getYear() == today.getYear()
                                            && occurrence.getOccurDate().getMonthValue() == today.getMonthValue()
                                            && occurrence.getReason() != null
                                            && occurrence.getReason().contains("신입 월차 자동 부여")
                            );

            // 이번 달 이미 월차가 있으면 제외
            if (alreadyGrantedThisMonth) {
                continue;
            }

            // 월차 1일 발생 내역 저장
            LeaveOccurrenceEntity occurrence =
                    LeaveOccurrenceEntity.builder()
                            .employee(employee)
                            .leaveType(annualLeaveType)
                            .targetYear(currentYear)
                            .occurDate(today)
                            .occurDays(new BigDecimal("1.0"))
                            .used_days(BigDecimal.ZERO)
                            .remain_days(new BigDecimal("1.0"))
                            .expiryDate(LocalDate.of(currentYear, 12, 31))
                            .reason("신입 월차 자동 부여")
                            .build();

            leaveOccurrenceRepository.save(occurrence);

            grantedCount++;
        }

        return grantedCount;
    }

    /**
     * 관리자 연차/휴가 현황 목록 조회
     *
     * keyword:
     * - 사원명 검색어
     *
     * deptId:
     * - 부서 검색 조건
     *
     * sortType:
     * - 정렬 조건
     *
     * page:
     * - 현재 페이지 번호
     * - Spring Data Page는 0부터 시작한다.
     *
     * size:
     * - 한 페이지당 조회할 데이터 개수
     */
    @Override
    public Page<AdLeaveStatusDTO> findAdminLeaveStatusList(
            String keyword,
            Integer deptId,
            String sortType,
            int page,
            int size
    ) {
        // PageRequest는 Pageable 구현체이다.
        // page: 현재 페이지 번호
        // size: 한 페이지당 데이터 개수
        Pageable pageable = PageRequest.of(page, size);

        // Repository에서 QueryDSL 페이징 조회 수행
        return leaveOccurrenceRepository.findAdminLeaveStatusList(
                keyword,
                deptId,
                sortType,
                pageable
        );
    }

    /**
     * 관리자 연차/휴가 현황 Excel 다운로드
     *
     * 처리 흐름:
     * 1. 현재 검색 조건을 그대로 사용한다.
     * 2. Excel 다운로드는 페이징 없이 전체 검색 결과를 내려받는다.
     * 3. 충분히 큰 size로 Page 조회를 수행한다.
     * 4. 조회 결과를 LeaveExcelExporter에 전달해 Excel byte 배열을 생성한다.
     */
    @Override
    public byte[] downloadLeaveExcel(
            String keyword,
            Integer deptId,
            String sortType
    ) {
        // Excel 다운로드는 전체 검색 결과를 대상으로 하므로 첫 페이지부터 충분히 크게 조회한다.
        int excelPage = 0;
        int excelSize = 10000;

        // 기존 관리자 연차 현황 조회 로직 재사용
        Page<AdLeaveStatusDTO> leaveStatusPage =
                findAdminLeaveStatusList(
                        keyword,
                        deptId,
                        sortType,
                        excelPage,
                        excelSize
                );

        // Excel에 출력할 전체 연차/휴가 목록
        List<AdLeaveStatusDTO> leaveList =
                leaveStatusPage.getContent();

        // Excel 생성 전용 클래스
        LeaveExcelExporter exporter =
                new LeaveExcelExporter();

        // Excel 파일 byte 배열 반환
        return exporter.export(leaveList);
    }
}