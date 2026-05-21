/*
 * [결재-근태 연동용]: 부재 일정 승인 시 ATTENDANCE.status에 휴가/반차 상태를 저장하기 위한 DB 보정 SQL입니다.
 *
 * 적용 배경:
 * - AttendanceStatus enum에 LEAVE("연차"), HALF_LEAVE("반차")를 추가했습니다.
 * - DB에 status 체크 제약이 걸려 있으면 새 값 저장이 실패할 수 있으므로 허용 값을 갱신합니다.
 */

ALTER TABLE attendance
    DROP CONSTRAINT IF EXISTS attendance_status_check;

ALTER TABLE attendance
    ADD CONSTRAINT attendance_status_check
        CHECK (status IN (
            'ON_TIME',
            'ABSENT',
            'LATE',
            'EARLY',
            'LEFT',
            'LEAVE',
            'HALF_LEAVE'
        ));
