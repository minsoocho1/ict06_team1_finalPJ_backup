package com.ict06.team1_fin_pj.domain.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * 공휴일 Entity
 *
 * HOLIDAY 테이블과 매핑된다.
 * 결근 자동 처리, 연차 계산, 공휴일 제외 로직에서 사용한다.
 */
@Entity
@Table(name = "holiday")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HolidayEntity {

    // 공휴일 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holiday_id")
    private Integer holidayId;

    // 공휴일 날짜
    // 동일 날짜의 공휴일은 중복 저장되지 않도록 DB에서 UNIQUE 처리되어 있다.
    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    // 공휴일명
    @Column(name = "holiday_name", nullable = false, length = 100)
    private String holidayName;

    // 사용 여부
    // true: 사용
    // false: 제외
    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    // 생성 일시
    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}