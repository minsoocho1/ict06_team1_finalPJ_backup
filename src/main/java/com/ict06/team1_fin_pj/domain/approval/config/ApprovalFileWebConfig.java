package com.ict06.team1_fin_pj.domain.approval.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 결재 첨부파일 정적 리소스 매핑 설정입니다.
 *
 * 결재 파일은 인사관리(employee) 파일과 섞이지 않도록 프로젝트 루트의
 * ict_06_uploads/approval 폴더에 저장하고, 브라우저에서는 /approval/uploads/** 경로로 접근합니다.
 */
@Configuration
public class ApprovalFileWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String approvalUploadPath = Paths.get(
                System.getProperty("user.dir"),
                "ict_06_uploads",
                "approval"
        ).toUri().toString();

        registry.addResourceHandler("/approval/uploads/**")
                .addResourceLocations(approvalUploadPath);
    }
}
