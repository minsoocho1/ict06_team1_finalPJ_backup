/**
 * @FileName : AiCorrectionService.java
 * @Description : AI 비서 문장 다듬기 Service 구현체
 *                - 사용자가 입력한 문장을 목적과 톤에 맞게 교정
 *                - 공손함, 논리성, 간결성 등 선택 조건 기반 문장 개선
 *                - Gemini 모델 호출을 통한 교정 결과 생성
 *                - 교정 전/후 문장 비교 및 응답 DTO 반환 처리
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진       최초 생성
 * @ 2026.05.12    송혜진       문장 다듬기 API 및 Gemini 호출 흐름 정리
 * @ 2026.05.28    송혜진       AI 비서 문장 교정 응답 구조 및 fallback 기준 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.CorrectionResponseDto;

public interface AiCorrectionService {

    CorrectionResponseDto correct(String empNo, String text, String mode);
}