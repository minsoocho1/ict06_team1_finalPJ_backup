package com.ict06.team1_fin_pj.domain.attendance.excel;

import com.ict06.team1_fin_pj.common.dto.attendance.AdLeaveStatusDTO;

import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;

import java.util.List;

/**
 * 관리자 연차/휴가 현황 Excel 생성 클래스
 *
 * 역할:
 * - 관리자 연차/휴가 조회 결과를 Excel(.xlsx) 파일로 생성한다.
 */
@Slf4j
public class LeaveExcelExporter {

    /**
     * 관리자 연차/휴가 현황 Excel 생성
     *
     * 반환값:
     * - 생성된 Excel 파일 byte 배열
     */
    public byte[] export(List<AdLeaveStatusDTO> leaveList) {

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

            // Excel Sheet 생성
            Sheet sheet =
                    workbook.createSheet("연차 현황");

            // Header 생성
            Row headerRow =
                    sheet.createRow(0);

            headerRow.createCell(0).setCellValue("사번");
            headerRow.createCell(1).setCellValue("사원명");
            headerRow.createCell(2).setCellValue("부서");
            headerRow.createCell(3).setCellValue("총 연차");
            headerRow.createCell(4).setCellValue("사용 연차");
            headerRow.createCell(5).setCellValue("잔여 연차");
            headerRow.createCell(6).setCellValue("사용 상태");

            // 데이터 시작 row
            int rowIdx = 1;

            // 연차 데이터 저장
            for (AdLeaveStatusDTO dto : leaveList) {

                Row row =
                        sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(dto.getEmpNo());
                row.createCell(1).setCellValue(dto.getEmpName());
                row.createCell(2).setCellValue(dto.getDeptName());

                row.createCell(3).setCellValue(
                        dto.getTotalDays() != null
                                ? dto.getTotalDays().doubleValue()
                                : 0
                );

                row.createCell(4).setCellValue(
                        dto.getUsedDays() != null
                                ? dto.getUsedDays().doubleValue()
                                : 0
                );

                row.createCell(5).setCellValue(
                        dto.getRemainDays() != null
                                ? dto.getRemainDays().doubleValue()
                                : 0
                );

                // 사용 상태 계산
                String usageStatus = "사용중";

                // 잔여 연차가 0 이하이면 소진 상태로 처리
                if (dto.getRemainDays() != null &&
                        dto.getRemainDays().doubleValue() <= 0) {

                    usageStatus = "소진";
                }

                // 사용 상태 저장
                row.createCell(6).setCellValue(usageStatus);
            }

            // 컬럼 너비 자동 조정
            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            // Excel 생성
            workbook.write(out);

            log.info("연차/휴가 Excel 생성 완료");

            return out.toByteArray();

        } catch (Exception e) {

            log.error("연차/휴가 Excel 생성 실패", e);

            throw new RuntimeException("Excel 생성 중 오류 발생");
        }
    }
}