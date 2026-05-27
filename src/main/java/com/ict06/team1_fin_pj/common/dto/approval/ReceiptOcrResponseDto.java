package com.ict06.team1_fin_pj.common.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 비용 정산 신청 화면에서 영수증 OCR 결과를 받을 때 사용하는 응답 DTO입니다.
 *
 * <p>네이버 CLOVA OCR 원본 응답은 필드가 많고 중첩 구조가 깊기 때문에
 * React 화면에서는 전자결재 서식 필드 id와 값만 알 수 있도록 단순화해서 내려줍니다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptOcrResponseDto {

    /**
     * OCR 호출 및 필드 매핑 성공 여부입니다.
     * false인 경우 message를 화면 안내 문구로 사용할 수 있습니다.
     */
    private boolean success;

    /**
     * 사용자 또는 개발자가 OCR 처리 결과를 이해하기 위한 메시지입니다.
     */
    private String message;

    /**
     * 전자결재 서식 필드 id를 key로 사용하는 자동 입력 값입니다.
     *
     * <p>현재 비용 정산 신청 서식에 맞춰 payment_date, expense_amount,
     * receipt_items_summary 값을 우선 채웁니다.</p>
     */
    private Map<String, String> fieldValues;
}
