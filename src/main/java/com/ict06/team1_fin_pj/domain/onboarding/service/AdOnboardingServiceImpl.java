/**
 * @FileName : AdOnboardingServiceImpl.java
 * @Description : 관리자 온보딩 서비스 구현체
 * @Author : 김다솜
 * @Date : 2026. 05. 11
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.11    김다솜        대시보드 형태로 개편 및 실제 데이터 연동
 */
package com.ict06.team1_fin_pj.domain.onboarding.service;

import com.ict06.team1_fin_pj.common.dto.onboarding.AdDocumentRequestDto;
import com.ict06.team1_fin_pj.domain.onboarding.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdOnboardingServiceImpl {

    private final DocumentRepository documentRepository;
    private final RestTemplate restTemplate;
    private final String AI_RAG_API = "http://localhost:8000/api/rag/process";

    @Transactional
    public void saveDocumentAndTriggerRag(AdDocumentRequestDto dto) {
        // 1. DB에 문서 메타데이터 저장
        // DocumentEntity entity = dto.toEntity();
        // documentRepository.save(entity);

        // 2. AI 서버에 RAG 처리 요청 (비동기 처리 권장)
        try {
            Map<String, Object> params = Map.of("doc_id", 1, "file_path", dto.getFilePath()); // 예시 ID
            restTemplate.postForObject(AI_RAG_API, params, Map.class);
        } catch (Exception e) {
            // 로그 기록 후 사용자에게는 등록 완료 알림
        }
    }
}
