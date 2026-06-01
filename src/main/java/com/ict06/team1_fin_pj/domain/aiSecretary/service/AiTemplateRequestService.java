/**
 * @FileName : AiTemplateRequestService.java
 * @Description : AI 추천 템플릿 요청 Service 구현체
 *                - 사용자의 추천 템플릿 추가 요청 저장 및 조회
 *                - 내 템플릿 요청 목록 조회
 *                - 요청 상태, 카테고리, 사용 목적, 요청 사유 관리
 *                - 관리자 템플릿 승인 관리 화면과 연계되는 요청 데이터 처리
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진       최초 생성
 * @ 2026.05.11    송혜진       추천 템플릿 요청 등록 및 내 요청 목록 조회 기능 정리
 * @ 2026.05.28    송혜진       관리자 추천 템플릿 승인 관리 연계 기준 반영
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.TemplateRequestCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.TemplateRequestResponseDto;

import java.util.List;

public interface AiTemplateRequestService {

    TemplateRequestResponseDto createRequest(TemplateRequestCreateRequestDto requestDto);

    List<TemplateRequestResponseDto> getMyRequests(String empNo);
}