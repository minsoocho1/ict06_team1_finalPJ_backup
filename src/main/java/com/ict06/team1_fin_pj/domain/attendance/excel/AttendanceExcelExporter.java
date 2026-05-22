package com.ict06.team1_fin_pj.domain.attendance.excel;

import com.ict06.team1_fin_pj.common.dto.attendance.AdAttendanceDTO;

import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;

import java.util.List;

/**
 * 관리자 근태 현황 Excel 생성 클래스
 *
 * 역할:
 * - 관리자 근태 조회 결과를 Excel(.xlsx) 파일로 생성한다.
 */
@Slf4j
public class AttendanceExcelExporter {

    /**
     * 관리자 근태 현황 Excel 생성
     *
     * 반환값:
     * - 생성된 Excel 파일 byte 배열
     */
    public byte[] export(List<AdAttendanceDTO> attendanceList) {

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

            // Excel Sheet 생성
            Sheet sheet =
                    workbook.createSheet("근태 현황");

            // 첫 번째 행(Header) 생성
            Row headerRow =
                    sheet.createRow(0);

            headerRow.createCell(0).setCellValue("사번");
            headerRow.createCell(1).setCellValue("사원명");
            headerRow.createCell(2).setCellValue("부서");
            headerRow.createCell(3).setCellValue("근무일");
            headerRow.createCell(4).setCellValue("출근시간");
            headerRow.createCell(5).setCellValue("퇴근시간");
            headerRow.createCell(6).setCellValue("근무시간");
            headerRow.createCell(7).setCellValue("상태");

            // 데이터 행 생성 시작 index
            int rowIdx = 1;

            // 조회된 근태 데이터를 한 줄씩 Excel에 저장
            for (AdAttendanceDTO dto : attendanceList) {

                Row row =
                        sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(dto.getEmpNo());
                row.createCell(1).setCellValue(dto.getEmpName());
                row.createCell(2).setCellValue(dto.getDeptName());

                // LocalDate / LocalDateTime null 체크
                row.createCell(3).setCellValue(
                        dto.getWorkDate() != null
                                ? dto.getWorkDate().toString()
                                : ""
                );

                // 출근시간
                row.createCell(4).setCellValue(
                        dto.getCheckIn() != null
                                ? dto.getCheckIn()
                                : ""
                );

                // 퇴근시간
                row.createCell(5).setCellValue(
                        dto.getCheckOut() != null
                                ? dto.getCheckOut()
                                : ""
                );

                row.createCell(6).setCellValue(
                        dto.getWorkHours() != null
                                ? dto.getWorkHours().toString()
                                : "0"
                );

                row.createCell(7).setCellValue(dto.getStatus());
            }

            // 컬럼 너비 자동 조정
            for (int i = 0; i < 8; i++) {
                sheet.autoSizeColumn(i);
            }

            // Excel 파일 생성
            workbook.write(out);

            log.info("근태 현황 Excel 생성 완료");

            return out.toByteArray();

        } catch (Exception e) {

            log.error("근태 현황 Excel 생성 실패", e);

            throw new RuntimeException("Excel 생성 중 오류 발생");
        }
    }
}