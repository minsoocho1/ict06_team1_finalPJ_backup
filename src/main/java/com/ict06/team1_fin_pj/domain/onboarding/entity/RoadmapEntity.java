/**
 * @FileName : RoadmapEntity.java
 * @Description : 온보딩 로드맵 Entity
 * @Author : 김다솜
 * @Date : 2026. 05. 02
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.02    김다솜        최초 생성
 * @ 2026.05.10    김다솜        관리자 로드맵 수정 처리를 위한 update 메서드 추가
 */
package com.ict06.team1_fin_pj.domain.onboarding.entity;

import com.ict06.team1_fin_pj.common.dto.BaseTimeEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.DepartmentEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.PositionEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ROADMAP")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadmapEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roadmap_id")
    private Integer roadmapId;

    @Column(length = 200, nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_no", nullable = false)
    private EmpEntity employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id")
    private DepartmentEntity department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private PositionEntity position;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_type", length = 20)
    private GeneratedType generatedType;

    @Builder.Default
    private Integer version = 1;

    @Builder.Default
    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RoadItemEntity> items = new ArrayList<>();

    public void updateRoadmap(String title, GeneratedType generatedType) {
        this.title = title;
        this.generatedType = generatedType;
    }

    public void updateCompletionStatus(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }
}
