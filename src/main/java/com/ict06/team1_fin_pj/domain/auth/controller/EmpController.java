/**
 * @FileName : EmpController.java
 * @Description : 사원 정보 조회 및 마이페이지 관리 컨트롤러
 * @Author : 김다솜
 * @Date : 2026. 04. 17
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.17    김다솜        최초 생성 및 웰컴페이지 정보 조회 추가
 * @ 2026.04.23    김다솜        마이페이지 정보 수정 메서드 추가
 * @ 2026.05.28    김다솜        마이페이지 프로필 사진 업로드 API 추가
 */

package com.ict06.team1_fin_pj.domain.auth.controller;

import com.ict06.team1_fin_pj.common.security.PrincipalDetails;
import com.ict06.team1_fin_pj.domain.auth.service.EmpServiceImpl;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class EmpController {

    private final EmpServiceImpl empService;

    // 웰컴페이지 > 로그인 계정 정보 조회
    @GetMapping("/welcome")
    public ResponseEntity<EmpEntity> getWelcomeInfo(Principal principal) {
        String loginEmpNo = principal.getName();

        EmpEntity emp = empService.getWelcomeInfo(loginEmpNo);
        return ResponseEntity.ok(emp);
    }

    // 새로고침 등 로그인 사용자 정보 복구
    @GetMapping("/me")
    public ResponseEntity<EmpEntity> getMyInfo(Principal principal) {
        String loginEmpNo = principal.getName();

        EmpEntity emp = empService.getWelcomeInfo(loginEmpNo);
        return ResponseEntity.ok(emp);
    }

    // 마이페이지 > 정보 수정
    @PutMapping("/update")
    public ResponseEntity<?> updateEmpInfo(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody EmpEntity emp
    ) {
        String empNo = principal.getUsername();
        empService.updateEmpInfo(empNo, emp.getName(), emp.getEmail(), emp.getPhone());
        return ResponseEntity.ok("수정 완료");
    }

    // 마이페이지 > 프로필 사진 업로드
    @PostMapping("/profile-image")
    public ResponseEntity<EmpEntity> updateProfileImage(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestParam("file") MultipartFile file
    ) {
        String empNo = principal.getUsername();
        EmpEntity updatedEmp = empService.updateProfileImage(empNo, file);
        return ResponseEntity.ok(updatedEmp);
    }
}
