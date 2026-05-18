/**
 * @FileName : AiAssistantDraftService.js
 * @Description : 보고서/회의록/결재 사유 초안 생성 흐름 담당
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.05.06    송혜진        최초 생성 (초안 생성/ 수정 메서드 추가)
 * @ 2026.05.11    송혜진        템플릿 생성 메서드 추가
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.*;

public interface AiAssistantDraftService {

    // 초안 생성
    AssistantDraftResponseDto createDraft(AssistantDraftRequestDto requestDto);

    // 초안 수정
    AssistantReviseResponseDto reviseDraft(AssistantReviseRequestDto requestDto);

    // AI 템플릿 생성
    AssistantTemplateResponseDto createTemplate(AssistantTemplateRequestDto requestDto);

}