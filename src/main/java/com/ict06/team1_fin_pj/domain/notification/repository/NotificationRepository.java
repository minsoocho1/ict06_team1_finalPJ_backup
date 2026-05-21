/**
 * @FileName : NotificationRepository.java
 * @Description : 알림 데이터(DB) 접근을 위한 JPA 레포지토리
 * @Author : 김다솜
 * @Date : 2026. 04. 23
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.23    김다솜        최초 생성 및 사원별 최신 알림 조회, 미읽음 카운트 추가
 * @ 2026.05.08    김다솜        알림 전체 읽음 및 삭제 처리를 위한 사원별 조회 메서드 추가
 * @ 2026.05.12    김다솜        온보딩 자동 알림 중복 발송 방지 조회 메서드 추가
 * @ 2026.05.18    김다솜        온보딩 사원-콘텐츠별 알림 발송 이력 조회 메서드 추가
 */
package com.ict06.team1_fin_pj.domain.notification.repository;

import com.ict06.team1_fin_pj.domain.notification.entity.NotificationEntity;
import com.ict06.team1_fin_pj.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Integer> {

    // 특정 사원의 알림 목록을 최신순으로 조회한다.
    List<NotificationEntity> findByEmployee_EmpNoOrderByCreatedAtDesc(String empNo);

    // 읽지 않은 알림 개수를 조회한다.
    long countByEmployee_EmpNoAndIsRead(String empNo, Boolean isRead);

    List<NotificationEntity> findByEmployee_EmpNoAndIsRead(String empNo, Boolean isRead);

    List<NotificationEntity> findByEmployee_EmpNo(String empNo);

    List<NotificationEntity> findByEmployee_EmpNoAndNotiTypeAndUrlOrderByCreatedAtDesc(
            String empNo,
            NotificationType notiType,
            String url
    );

    boolean existsByEmployee_EmpNoAndNotiTypeAndTitleAndUrl(
            String empNo,
            NotificationType notiType,
            String title,
            String url
    );
}
