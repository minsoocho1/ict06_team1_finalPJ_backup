package com.ict06.team1_fin_pj.test.repository;

import com.ict06.team1_fin_pj.test.entity.TestEntity;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 테스트 예제용 QueryDSL 구현체입니다.
 * QTestEntity 생성 여부에 애플리케이션 기동이 의존하지 않도록 PathBuilder를 사용합니다.
 */
@Repository
@RequiredArgsConstructor
public class TestRepositoryImpl implements TestRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<TestEntity> selectTestList() {
        PathBuilder<TestEntity> test =
                new PathBuilder<>(TestEntity.class, "testEntity");

        return queryFactory
                .selectFrom(test)
                .orderBy(test.getNumber("formId", Integer.class).desc())
                .fetch();
    }
}
