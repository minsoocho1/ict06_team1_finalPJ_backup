/**
 * @FileName : ChecklistController.java
 * @Description : AI 온보딩 체크리스트 Controller
 *                체크리스트 조회 및 완료 처리 API 제공
 * @Author : 김다솜
 * @Date : 2026. 04. 29
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.29    김다솜        최초 생성/체크리스트 조회 및 완료 처리 API 구현
 * @ 2026.05.15    김다솜        체크리스트 완료/취소 제한 메시지 응답 처리 추가
 */

package com.ict06.team1_fin_pj.domain.onboarding.controller;

import com.ict06.team1_fin_pj.common.dto.onboarding.ChecklistCompleteRequest;
import com.ict06.team1_fin_pj.common.dto.onboarding.ChecklistResponse;
import com.ict06.team1_fin_pj.domain.onboarding.service.ChecklistServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding/checklist")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ChecklistController {

    private final ChecklistServiceImpl checklistService;

    /**
     * 체크리스트 조회
     * :param empNo 사번
     * :return 체크리스트 목록
     */
    @GetMapping("/{empNo}")
    public ResponseEntity<List<ChecklistResponse>> getChecklist(@PathVariable String empNo) {
        List<ChecklistResponse> checklist = checklistService.getChecklist(empNo);
        return ResponseEntity.ok(checklist);
    }

    /**
     * 체크리스트 완료 처리
     * :param request 완료 요청 DTO
     */
    @PostMapping("/complete")
    public ResponseEntity<String> completeChecklist(@RequestBody ChecklistCompleteRequest request) {
        try {
            checklistService.completeChecklist(request.getEmpNo(), request.getChecklistId());
            return ResponseEntity.ok("체크리스트 완료 처리 성공");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 체크리스트 미완료 처리
     * :param request 완료 요청 DTO
     */
    @PostMapping("/uncomplete")
    public ResponseEntity<String> uncompleteChecklist(@RequestBody ChecklistCompleteRequest request) {
        try {
            checklistService.uncompleteChecklist(request.getEmpNo(), request.getChecklistId());
            return ResponseEntity.ok("체크리스트 미완료 처리 성공");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


}
