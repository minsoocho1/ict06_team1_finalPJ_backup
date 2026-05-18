package com.ict06.team1_fin_pj.domain.approval.repository;

import com.ict06.team1_fin_pj.common.dto.approval.ApprovalListResponseDto;
import com.ict06.team1_fin_pj.domain.approval.entity.ApprovalStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ict06.team1_fin_pj.domain.approval.entity.QAppLineEntity.appLineEntity;
import static com.ict06.team1_fin_pj.domain.approval.entity.QApprovalEntity.approvalEntity;

/**
 * 결재 문서 목록 조회를 담당하는 QueryDSL Repository 구현체입니다.
 *
 * 개인 문서함, 임시저장함, 참조 문서함, 결재 대기/예정 문서함은 조회 조건이 서로 다릅니다.
 * 조건 조립과 DTO 직접 조회를 명시적으로 관리하기 위해 QueryDSL을 사용합니다.
 */
@Repository
@RequiredArgsConstructor
public class ApprovalRepositoryCustomImpl implements ApprovalRepositoryCustom {

    /*
     * QueryDSL 쿼리를 생성하는 핵심 객체입니다.
     * 조건 조립, DTO 직접 조회, 페이징 쿼리를 안전하게 작성할 수 있습니다.
     */
    private final JPAQueryFactory queryFactory;

    /**
     * 로그인 사용자가 작성한 결재 문서 목록을 조회합니다.
     *
     * status가 있으면 해당 상태만 조회하고,
     * status가 없으면 임시저장(DRAFT)을 제외한 모든 문서를 조회합니다.
     */
    @Override
    public Page<ApprovalListResponseDto> findMyDocuments(
            String writerNo,
            ApprovalStatus status,
            Pageable pageable
    ) {
        BooleanBuilder builder = baseWriterCondition(writerNo);

        if (status != null) {
            builder.and(approvalEntity.status.eq(status));
        } else {
            builder.and(approvalEntity.status.ne(ApprovalStatus.DRAFT));
        }

        return fetchApprovalPage(builder, pageable);
    }

    /**
     * 로그인 사용자가 임시저장한 문서만 조회합니다.
     */
    @Override
    public Page<ApprovalListResponseDto> findMyDrafts(
            String writerNo,
            Pageable pageable
    ) {
        BooleanBuilder builder = baseWriterCondition(writerNo)
                .and(approvalEntity.status.eq(ApprovalStatus.DRAFT));

        return fetchApprovalPage(builder, pageable);
    }

    /**
     * 로그인 사용자가 참조자로 포함된 결재 문서 목록을 조회합니다.
     *
     * stepOrder=0은 결재 순서에 참여하지 않는 참조 대상을 의미합니다.
     * 참조자는 임시저장 문서까지 볼 수 있으면 안 되므로 DRAFT 상태는 항상 제외합니다.
     */
    @Override
    public Page<ApprovalListResponseDto> findMyReferencedDocuments(
            String referenceNo,
            ApprovalStatus status,
            Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder()
                .and(appLineEntity.approver.empNo.eq(referenceNo))
                .and(appLineEntity.stepOrder.eq(0))
                .and(approvalEntity.isDeleted.isFalse());

        if (status != null) {
            builder.and(approvalEntity.status.eq(status));
        }

        builder.and(approvalEntity.status.ne(ApprovalStatus.DRAFT));

        return fetchLineParticipantPage(builder, pageable);
    }

    /**
     * 현재 로그인 사용자가 지금 결재해야 하는 문서 목록을 조회합니다.
     *
     * 현재 결재자(currentApprover)가 로그인 사용자이고,
     * 문서 상태가 진행 중(IN_PROGRESS)인 문서만 결재 대기 문서입니다.
     */
    @Override
    public Page<ApprovalListResponseDto> findPendingApprovals(
            String approverNo,
            Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder()
                .and(approvalEntity.currentApprover.empNo.eq(approverNo))
                .and(approvalEntity.status.eq(ApprovalStatus.IN_PROGRESS))
                .and(approvalEntity.isDeleted.isFalse());

        return fetchApprovalPage(builder, pageable);
    }

    /**
     * 로그인 사용자가 결재선에는 포함되어 있지만 아직 차례가 오지 않은 문서 목록을 조회합니다.
     *
     * stepOrder가 현재 결재 단계보다 큰 경우가 "앞 단계 결재가 끝나면 내 차례가 올 문서"입니다.
     * stepOrder=0인 참조자는 결재자가 아니므로 예정 문서함에서 제외합니다.
     */
    @Override
    public Page<ApprovalListResponseDto> findUpcomingApprovals(
            String approverNo,
            Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder()
                .and(appLineEntity.approver.empNo.eq(approverNo))
                .and(appLineEntity.stepOrder.gt(0))
                .and(appLineEntity.stepOrder.gt(approvalEntity.currentStep))
                .and(approvalEntity.status.eq(ApprovalStatus.IN_PROGRESS))
                .and(approvalEntity.isDeleted.isFalse());

        return fetchLineParticipantPage(builder, pageable);
    }

    /**
     * 개인 문서함 계열 조회에서 항상 적용하는 공통 조건입니다.
     */
    private BooleanBuilder baseWriterCondition(String writerNo) {
        return new BooleanBuilder()
                .and(approvalEntity.writer.empNo.eq(writerNo))
                .and(approvalEntity.isDeleted.isFalse());
    }

    /**
     * APP_LINE 조인이 필요 없는 결재 문서 목록 조회를 수행합니다.
     */
    private Page<ApprovalListResponseDto> fetchApprovalPage(
            BooleanBuilder builder,
            Pageable pageable
    ) {
        List<ApprovalListResponseDto> content = baseListProjection()
                .from(approvalEntity)
                .leftJoin(approvalEntity.form)
                .leftJoin(approvalEntity.writer)
                .leftJoin(approvalEntity.currentApprover)
                .where(builder)
                .orderBy(approvalEntity.updatedAt.desc(), approvalEntity.approvalId.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long countResult = queryFactory
                .select(approvalEntity.count())
                .from(approvalEntity)
                .where(builder)
                .fetchOne();

        long total = countResult != null ? countResult : 0;

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * APP_LINE 조인이 필요한 결재자/참조자 기준 목록 조회를 수행합니다.
     *
     * 한 문서에 같은 사용자가 여러 줄로 들어갈 가능성까지 고려해 distinct로 중복 문서를 제거합니다.
     */
    private Page<ApprovalListResponseDto> fetchLineParticipantPage(
            BooleanBuilder builder,
            Pageable pageable
    ) {
        List<ApprovalListResponseDto> content = queryFactory
                .selectDistinct(Projections.fields(
                        ApprovalListResponseDto.class,
                        approvalEntity.approvalId,
                        approvalEntity.form.formId,
                        approvalEntity.form.formName,
                        approvalEntity.writer.empNo.as("writerNo"),
                        approvalEntity.writer.name.as("writerName"),
                        approvalEntity.title,
                        approvalEntity.status,
                        approvalEntity.currentStep,
                        approvalEntity.maxStep,
                        approvalEntity.currentApprover.empNo.as("currentApproverNo"),
                        approvalEntity.currentApprover.name.as("currentApproverName"),
                        approvalEntity.createdAt,
                        approvalEntity.updatedAt
                ))
                .from(approvalEntity)
                .join(approvalEntity.lines, appLineEntity)
                .leftJoin(approvalEntity.form)
                .leftJoin(approvalEntity.writer)
                .leftJoin(approvalEntity.currentApprover)
                .where(builder)
                .orderBy(approvalEntity.updatedAt.desc(), approvalEntity.approvalId.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long countResult = queryFactory
                .select(approvalEntity.approvalId.countDistinct())
                .from(approvalEntity)
                .join(approvalEntity.lines, appLineEntity)
                .where(builder)
                .fetchOne();

        long total = countResult != null ? countResult : 0;

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 목록 화면에서 공통으로 사용하는 DTO projection입니다.
     */
    private com.querydsl.jpa.impl.JPAQuery<ApprovalListResponseDto> baseListProjection() {
        return queryFactory.select(Projections.fields(
                ApprovalListResponseDto.class,
                approvalEntity.approvalId,
                approvalEntity.form.formId,
                approvalEntity.form.formName,
                approvalEntity.writer.empNo.as("writerNo"),
                approvalEntity.writer.name.as("writerName"),
                approvalEntity.title,
                approvalEntity.status,
                approvalEntity.currentStep,
                approvalEntity.maxStep,
                approvalEntity.currentApprover.empNo.as("currentApproverNo"),
                approvalEntity.currentApprover.name.as("currentApproverName"),
                approvalEntity.createdAt,
                approvalEntity.updatedAt
        ));
    }
}
