package com.ict06.team1_fin_pj.common.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * @author : 송영은$
 * description : 결재서식 목록 화면용 Dto
 * ========================================
 * DATE         AUTHOR      NOTE
 * 2026-05-11   송영은       최초 생성
 **/
@Getter
@Builder
@AllArgsConstructor
public class AppFormListDto {

    private Integer formId;

    private String formName;

    private Boolean isDefault;

    private LocalDateTime updatedAt;

    // 연결된 결재선 정보
    private Integer lineTemplateId;

    private String lineTemplateName;
}
