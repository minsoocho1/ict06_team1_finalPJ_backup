package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiTemplateEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiTemplateRepository extends JpaRepository<AiTemplateEntity, Integer> {

    List<AiTemplateEntity> findByIsActiveTrueOrderByCreatedAtDesc();

    List<AiTemplateEntity> findByTypeAndIsActiveTrueOrderByCreatedAtDesc(DocumentType type);
}