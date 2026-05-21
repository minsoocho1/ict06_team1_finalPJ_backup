/**
 * @FileName : RoadmapRepository.java
 * @Description : 온보딩 로드맵 Repository
 * 사원별 최신 로드맵 조회 기능을 제공
 * @Author : 김다솜
 * @Date : 2026. 05. 07
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.07    김다솜        최초 생성 및 사원 번호 기준 최신 로드맵 조회 메서드 구현
 */

package com.ict06.team1_fin_pj.domain.onboarding.repository;

import com.ict06.team1_fin_pj.domain.onboarding.entity.RoadmapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoadmapRepository extends JpaRepository<RoadmapEntity, Integer> {

    Optional<RoadmapEntity> findFirstByEmployee_EmpNoOrderByRoadmapIdDesc(String empNo);

}
