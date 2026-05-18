package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AiTemplateDashboardRepository extends JpaRepository<AiTemplateEntity, Integer> {

    long countByCreatedAtBetween(LocalDateTime startAt, LocalDateTime endAt);
}
