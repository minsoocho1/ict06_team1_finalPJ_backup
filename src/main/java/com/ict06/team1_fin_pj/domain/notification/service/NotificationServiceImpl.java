/**
 * @FileName : NotificationServiceImpl.java
 * @Description : SSE 기반 실시간 알림 전송 및 알림 상태 관리 서비스
 * @Author : 김다솜
 * @Date : 2026. 04. 23
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.23    김다솜        최초 생성
 * @ 2026.04.23    김다솜        SSE 연결 관리 및 알림 저장·전송 로직/더티 체킹 이용한 알림 읽음 처리 로직 추가
 * @ 2026.05.08    김다솜        Redis 장애 시 DB 기반 안 읽은 알림 수 조회로 fallback 처리/알림 전체 읽음, 단건 삭제, 전체 삭제 기능 추가
 * @ 2026.05.19    김다솜        RAG 처리 실패 알림 수신자 조회를 관리자 role_id 기준으로 보강
 * @ 2026.05.19    김다솜        RAG 처리 실패 알림에 문서 제목 및 문서/RAG 관리 상세 위치 이동 URL 추가
 */

package com.ict06.team1_fin_pj.domain.notification.service;

import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.notification.entity.NotificationEntity;
import com.ict06.team1_fin_pj.domain.notification.entity.NotificationType;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.notification.repository.NotificationRepository;
import com.ict06.team1_fin_pj.domain.notification.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl {

    private final NotificationRepository notificationRepository;
    private final SseEmitterManager sseEmitterManager;
    private final EmpRepository empRepository;
    private final RedisTemplate<String, String> redisTemplate;

    //SSE 구독(프론트에서 처음 연결 시 추출)
    public SseEmitter subscribe(String empNo) {
        SseEmitter emitter = new SseEmitter(60*60*1000L);
        sseEmitterManager.add(empNo, emitter);

        //연결 종료/타임아웃 시 제거
        emitter.onCompletion(() -> sseEmitterManager.remove(empNo));
        emitter.onTimeout(() -> sseEmitterManager.remove(empNo));
        emitter.onError(e -> sseEmitterManager.remove(empNo));

        //연결 직후 더미 이벤트 전송(브라우저 연결 유지용)
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch(Exception e) {
            sseEmitterManager.remove(empNo);
        }
        return emitter;
    }

    //알림 저장 + 실시간 전송
    //다른 도메인에서 알림 기능 필요할 때 이 메서드 호출하면 됩니다!
    @Transactional
    public void sendNotification(String empNo, String notiType, String title, String content, String url) {
        EmpEntity emp = empRepository.findById(empNo)
                .orElseThrow(() -> new RuntimeException("직원을 찾을 수 없습니다."));

        //1. DB 저장
        NotificationEntity noti = NotificationEntity.builder()
                .employee(emp)
                .notiType(NotificationType.valueOf(notiType))
                .title(title)
                .content(content)
                .url(url)
                .isRead(false)
                .build();
        notificationRepository.save(noti);

        incrementUnreadCount(empNo);

        //3. SSE로 실시간 전송
        Map<String, Object> payload = Map.of(
                "notiId", noti.getNotiId(),
                "title", noti.getTitle(),
                "content", noti.getContent(),
                "url", noti.getUrl() != null ? noti.getUrl() : ""
        );
        sseEmitterManager.sendToEmp(empNo, payload);
    }

    /**
     * 관리자용: RAG 문서 처리 실패 알림 발송
     * @param docId 문서 ID
     * @param title 문서 제목
     * @param stage 실패한 단계 (예: "청크 분할", "벡터 임베딩")
     */
    @Transactional
    public void sendRagFailureNotification(Integer docId, String title, String stage) {
        String documentTitle = title != null && !title.isBlank() ? title : "제목 없음";
        String content = String.format(
                "문서명: %s / 실패 단계: %s. 온보딩 관리 > 문서/RAG 관리에서 해당 문서를 확인해 주세요.",
                documentTitle,
                stage
        );
        String url = docId != null
                ? "/admin/onboarding/documents#document-" + docId
                : "/admin/onboarding/documents";

        // 모든 관리자 권한 계정 조회
        List<EmpEntity> admins = findAdminEmployees();
        for (EmpEntity admin : admins) {
            this.sendNotification(admin.getEmpNo(), NotificationType.ONBOARDING.name(), "문서 RAG 처리 실패 - " + documentTitle, content, url);
        }
    }

    /**
     * 관리자용: 사원 로드맵 학습 및 평가 완료 알림 발송
     * @param empNo 완료한 사원의 사번
     * @param empName 완료한 사원의 이름
     */
    @Transactional
    public void sendOnboardingCompleteNotification(String empNo, String empName) {
        String content = String.format("%s 사원이 온보딩 로드맵 학습 및 평가를 모두 완료했습니다. 최종 성취도를 확인해 주세요.", empName);
        String url = "/admin/onboarding/main";

        List<EmpEntity> admins = findAdminEmployees();
        for (EmpEntity admin : admins) {
            this.sendNotification(admin.getEmpNo(), NotificationType.ONBOARDING.name(), "온보딩 완료 보고", content, url);
        }
    }

    private List<EmpEntity> findAdminEmployees() {
        Map<String, EmpEntity> admins = new LinkedHashMap<>();

        empRepository.findByRole_RoleId(1)
                .forEach(emp -> admins.put(emp.getEmpNo(), emp));
        empRepository.findByRole_RoleName("관리자")
                .forEach(emp -> admins.put(emp.getEmpNo(), emp));
        empRepository.findByRole_RoleName("ROLE_ADMIN")
                .forEach(emp -> admins.put(emp.getEmpNo(), emp));
        empRepository.findByRole_RoleName("ADMIN")
                .forEach(emp -> admins.put(emp.getEmpNo(), emp));

        return admins.values().stream().toList();
    }

    // 읽지 않은 알림 개수 조회: Redis 장애 또는 캐시 누락 시 DB 조회로 대체
    public long getUnreadCount(String empNo) {
        try {
            String redisKey = unreadCountKey(empNo);
            String count = redisTemplate.opsForValue().get(redisKey);

            if(count == null) {
                long dbCount = getUnreadCountFromDb(empNo);
                cacheUnreadCount(empNo, dbCount);
                return dbCount;
            }

            return Long.parseLong(count);
        } catch (NumberFormatException e) {
            long dbCount = getUnreadCountFromDb(empNo);
            cacheUnreadCount(empNo, dbCount);
            return dbCount;
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            return getUnreadCountFromDb(empNo);
        }
    }

    //알림 목록 조회
    public List<NotificationEntity> getNotifications(String empNo) {
        return notificationRepository.findByEmployee_EmpNoOrderByCreatedAtDesc(empNo);
    }


    //알림 읽음 처리 -> Redis 개수 감소 추가
    @Transactional
    public void markAsRead(String empNo, Integer notiId) {
        //1. DB에서 해당 알림 조회
        //persistence context에 스냅샷 저장
        NotificationEntity noti = notificationRepository.findById(notiId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));

        if(!noti.getEmployee().getEmpNo().equals(empNo)) {
            throw new RuntimeException("본인의 알림만 처리할 수 있습니다.");
        }

        //추가
        if(Boolean.FALSE.equals(noti.getIsRead())) {
            noti.markRead();

            decrementUnreadCount(empNo);
        }
    }

    @Transactional
    public void markAsRead(Integer notiId) {
        NotificationEntity noti = notificationRepository.findById(notiId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));
        markAsRead(noti.getEmployee().getEmpNo(), notiId);
    }

    @Transactional
    public void markAllAsRead(String empNo) {
        List<NotificationEntity> unreadNotifications =
                notificationRepository.findByEmployee_EmpNoAndIsRead(empNo, false);

        unreadNotifications.forEach(NotificationEntity::markRead);
        cacheUnreadCount(empNo, 0);
    }

    @Transactional
    public void deleteNotification(String empNo, Integer notiId) {
        NotificationEntity noti = notificationRepository.findById(notiId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));

        if(!noti.getEmployee().getEmpNo().equals(empNo)) {
            throw new RuntimeException("본인의 알림만 삭제할 수 있습니다.");
        }

        if(Boolean.FALSE.equals(noti.getIsRead())) {
            decrementUnreadCount(empNo);
        }
        notificationRepository.delete(noti);
    }

    @Transactional
    public void deleteAllNotifications(String empNo) {
        List<NotificationEntity> notifications = notificationRepository.findByEmployee_EmpNo(empNo);
        notificationRepository.deleteAll(notifications);
        cacheUnreadCount(empNo, 0);
    }

    private String unreadCountKey(String empNo) {
        return "unread:count:" + empNo;
    }

    private long getUnreadCountFromDb(String empNo) {
        return notificationRepository.countByEmployee_EmpNoAndIsRead(empNo, false);
    }

    private void cacheUnreadCount(String empNo, long count) {
        try {
            String redisKey = unreadCountKey(empNo);
            redisTemplate.opsForValue().set(redisKey, String.valueOf(count));
            redisTemplate.expire(redisKey, 30, TimeUnit.DAYS);
        } catch (RedisConnectionFailureException | RedisSystemException ignored) {
            // Redis 캐시는 보조 수단이므로 장애 시 DB 값을 그대로 사용한다.
        }
    }

    private void incrementUnreadCount(String empNo) {
        try {
            String redisKey = unreadCountKey(empNo);
            redisTemplate.opsForValue().increment(redisKey);
            redisTemplate.expire(redisKey, 30, TimeUnit.DAYS);
        } catch (RedisConnectionFailureException | RedisSystemException ignored) {
            // 알림 저장은 DB가 기준이므로 Redis 증가 실패는 무시한다.
        }
    }

    private void decrementUnreadCount(String empNo) {
        try {
            String redisKey = unreadCountKey(empNo);
            Long count = redisTemplate.opsForValue().decrement(redisKey);
            if(count != null && count < 0) {
                redisTemplate.opsForValue().set(redisKey, "0");
            }
        } catch (RedisConnectionFailureException | RedisSystemException ignored) {
            // 읽음 처리는 DB가 기준이므로 Redis 감소 실패는 무시한다.
        }
    }
}
