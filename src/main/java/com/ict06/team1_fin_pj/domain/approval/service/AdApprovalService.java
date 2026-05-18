package com.ict06.team1_fin_pj.domain.approval.service;

import com.ict06.team1_fin_pj.common.dto.approval.AppFormDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppFormListDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppLineFormDetailDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppLineFormListDto;
import com.ict06.team1_fin_pj.common.dto.approval.AppLineFormRequestDto;
import com.ict06.team1_fin_pj.common.security.PrincipalDetails;
import com.ict06.team1_fin_pj.domain.approval.entity.AppFormEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 관리자용 전자결재 설정 서비스 인터페이스입니다.
 * 결재 서식과 결재선 서식의 CRUD 및 연결 설정을 담당합니다.
 */
public interface AdApprovalService {

    void saveAppForm(AppFormEntity entity);

    List<AppFormEntity> listAllAppForms();

    Page<AppFormListDto> getAppFormsWithPaging(int page, int size, String keyword);

    AppFormEntity selectAppForm(int id);

    void deleteAppForm(int id);

    void deleteAppForms(List<Integer> ids);

    void updateAppForm(int id, AppFormDto dto);

    void saveAppLineForm(AppLineFormRequestDto dto, PrincipalDetails principal);

    Page<AppLineFormListDto> listAppLineForm(Pageable pageable);

    List<AppLineFormListDto> listAllAppLineTemplates();

    void applyLineTemplate(Integer formId, Integer templateId);

    void applyLineTemplateToForms(List<Integer> formIds, Integer templateId);

    Page<AppLineFormListDto> getAppLineFormsWithPaging(int page, int size, String keyword);

    AppLineFormDetailDto selectAppLineForm(Integer id);

    void deleteAppLineTemplate(int id);

    void updateAppLineForm(
            Integer templateId,
            AppLineFormRequestDto dto,
            PrincipalDetails principal
    );
}
