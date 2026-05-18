/**
 * @FileName : DocumentProcessingAsyncService.java
 * @Description : 문서 자동 처리 비동기 실행 Service
 * @Author : 김다솜
 * @Date : 2026. 05. 12
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -----------------------------------------------
 * @ 2026.05.12    김다솜        문서 재처리 요청 비동기 실행 로직 추가
 */

package com.ict06.team1_fin_pj.domain.onboarding.service;

import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentProcessingAsyncService {

    private final DocumentProcessingService documentProcessingService;

    @Async
    public void processDocumentAsync(Integer docId, EmpEntity processedBy) {
        documentProcessingService.processDocument(docId, processedBy);
    }
}
