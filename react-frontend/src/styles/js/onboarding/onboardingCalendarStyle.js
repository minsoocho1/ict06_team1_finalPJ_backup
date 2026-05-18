/**
 * @FileName : onboardingCalendarStyle.js
 * @Description : 캘린더 내 온보딩(로드맵) 일정 전용 스타일 정의
 * @Author : 김다솜
 * @Date : 2026. 05. 14
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.14    김다솜        최초 생성 및 온보딩 일정 CSS 정의
 */
export const onboardingCalendarStyle = {
    eventClass: 'event-onboarding',
    css: `
        .event-onboarding {
            background-color: #e8f5e9 !important;
            border: none !important;
            border-left: 4px solid #2e7d32 !important;
            color: #1b5e20 !important;
            font-weight: 600 !important;
            box-shadow: 0 2px 4px rgba(0,0,0,0.05);
            cursor: pointer;
            transition: transform 0.1s ease-in-out;
        }
        /* FullCalendar 내부 텍스트 요소 색상 강제 지정 */
        .event-onboarding .fc-event-main,
        .event-onboarding .fc-event-title,
        .event-onboarding .fc-event-time {
            color: #1b5e20 !important;
        }
        .event-onboarding:hover {
            transform: scale(1.02);
            filter: brightness(0.95);
        }
    `
};