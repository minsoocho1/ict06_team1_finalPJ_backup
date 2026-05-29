/**
 * @FileName : EmpServiceImpl.java
 * @Description : 사원 정보 조회 및 수정 처리 비즈니스 로직
 * @Author : 김다솜
 * @Date : 2026. 04. 23
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.23    김다솜        최초 생성 및 웰컴페이지 정보 조회, 마이페이지 정보 수정 구현
 * @ 2026.05.28    김다솜        마이페이지 프로필 사진 업로드 및 저장 경로 갱신 기능 추가
 * @ 2026.05.29    김다솜        관리자 사원 이미지 업로드 방식과 동일하게 JPG/PNG 형식만 허용
 */

package com.ict06.team1_fin_pj.domain.auth.service;

import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.notification.service.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmpServiceImpl {

    private static final Set<String> ALLOWED_PROFILE_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );

    private final EmpRepository empRepository;
    private final NotificationServiceImpl notificationService;

    // 웰컴페이지 정보 조회
    public EmpEntity getWelcomeInfo(String empNo) {
        return empRepository.findLoginEmpInfByEmpNo(empNo)
                .orElseThrow(() -> new RuntimeException(empNo + " 사번을 가진 사원의 정보가 없습니다."));
    }

    // 마이페이지 정보 수정
    public void updateEmpInfo(String empNo, String name, String email, String phone) {
        System.out.println("[EmpService] 정보 수정 요청 - empNo: " + empNo);
        empRepository.updateEmpInfo(empNo, name, email, phone);
        System.out.println("[EmpService] 알림 전송 시도 - empNo: " + empNo);

        notificationService.sendNotification(
                empNo,
                "MYPAGE",
                "정보 수정 알림",
                name + "님의 정보가 성공적으로 수정되었습니다.",
                "/auth/mypage"
        );
        System.out.println("[EmpService] 알림 전송 완료");
    }

    // 마이페이지 프로필 사진 업로드
    @Transactional
    public EmpEntity updateProfileImage(String empNo, MultipartFile file) {
        validateProfileImage(file);

        EmpEntity employee = empRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new RuntimeException(empNo + " 사번을 가진 사원의 정보가 없습니다."));

        String profileImgPath = saveProfileImage(file, empNo);
        employee.changeProfileImg(profileImgPath);

        return getWelcomeInfo(empNo);
    }

    private void validateProfileImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 프로필 사진을 선택해 주세요.");
        }

        if (!ALLOWED_PROFILE_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("프로필 사진은 JPG, PNG 형식만 업로드할 수 있습니다.");
        }
    }

    private String saveProfileImage(MultipartFile file, String empNo) {
        try {
            Path uploadDir = Paths.get(
                    System.getProperty("user.dir"),
                    "employee",
                    "ict_06_uploads",
                    "profile"
            );

            Files.createDirectories(uploadDir);

            String extension = getExtension(file.getOriginalFilename());
            String savedFileName = empNo + "_" + UUID.randomUUID() + extension;
            Path savePath = uploadDir.resolve(savedFileName);

            file.transferTo(savePath.toFile());

            return "/employee/uploads/profile/" + savedFileName;
        } catch (IOException e) {
            throw new RuntimeException("프로필 사진 저장 중 오류가 발생했습니다.", e);
        }
    }

    private String getExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return ".jpg";
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return extension.toLowerCase(Locale.ROOT);
    }
}
