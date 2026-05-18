/**
 * @FileName : OnContentEntity.java
 * @Description : 온보딩 학습 콘텐츠 Entity
 * @Author : 김다솜
 * @Date : 2026. 05. 08
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.08    김다솜        관리자 콘텐츠 수정 처리 및 tags jsonb 컬럼 저장 오류 방지를 위한 JSON 저장 매핑 추가
 * @ 2026.05.10    김다솜        콘텐츠별 대상 직급/부서 다중 선택을 위한 매핑 추가
 */
package com.ict06.team1_fin_pj.domain.onboarding.entity;

import com.ict06.team1_fin_pj.common.dto.BaseTimeEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.DepartmentEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.PositionEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "ON_CONTENT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnContentEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_id")
    private Integer contentId;

    @Column(length = 255, nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ContentType type;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "sub_category", length = 50)
    private String subCategory;

    @Column(name = "target_position", length = 50)
    private String targetPosition;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ON_CONTENT_TARGET_POSITION",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "position_id")
    )
    @Builder.Default
    private Set<PositionEntity> targetPositions = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ON_CONTENT_TARGET_DEPARTMENT",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "dept_id")
    )
    @Builder.Default
    private Set<DepartmentEntity> targetDepartments = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 20, nullable = false)
    private Difficulty difficulty;

    @Column(name = "estimated_time")
    private Integer estimatedTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private String tags;

    @Builder.Default
    @Column(name = "is_mandatory")
    private Boolean isMandatory = false;

    @Column(length = 500)
    private String path;

    public void updateContent(
            String title,
            ContentType type,
            String category,
            String subCategory,
            String targetPosition,
            Difficulty difficulty,
            Integer estimatedTime,
            String path,
            Boolean isMandatory
    ) {
        this.title = title;
        this.type = type;
        this.category = category;
        this.subCategory = subCategory;
        this.targetPosition = targetPosition;
        this.difficulty = difficulty;
        this.estimatedTime = estimatedTime;
        this.path = path;
        this.isMandatory = Boolean.TRUE.equals(isMandatory);
    }

    public void updateTargetPositions(Collection<PositionEntity> targetPositions) {
        this.targetPositions.clear();

        if (targetPositions != null) {
            this.targetPositions.addAll(targetPositions);
        }
    }

    public void updateTargetDepartments(Collection<DepartmentEntity> targetDepartments) {
        this.targetDepartments.clear();

        if (targetDepartments != null) {
            this.targetDepartments.addAll(targetDepartments);
        }
    }
}
