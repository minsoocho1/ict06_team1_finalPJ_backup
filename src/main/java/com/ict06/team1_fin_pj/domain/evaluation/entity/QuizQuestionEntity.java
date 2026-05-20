/**
 * @FileName : QuizQuestionEntity.java
 * @Description : AI 온보딩 퀴즈 문항 Entity
 *                - QUIZ_QUESTION 테이블 매핑
 *                - 학습 카테고리별 퀴즈 문항 정보 저장
 *                - 객관식, 단답형, 서술형 문항 및 AI 채점 기준 답안, 키워드, 루브릭 정보 관리
 * @Author : 김다솜
 * @Date : 2026. 04. 30
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.30    김다솜        최초 생성 및 AI 확장형 퀴즈 문항 Entity 구현
 * @ 2026.05.01    김다솜        카테고리별 퀴즈 생성을 위한 categoryName 필드 추가
 */

package com.ict06.team1_fin_pj.domain.evaluation.entity;

import com.ict06.team1_fin_pj.domain.onboarding.entity.OnContentEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "QUIZ_QUESTION")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Integer questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private OnContentEntity content;

    @Column(name = "category_name", length = 50, nullable = false)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 20, nullable = false)
    private QuestionType questionType;

    @Column(name = "question_text", length = 1000, nullable = false)
    private String questionText;

    @Column(name = "option_1", length = 500)
    private String option1;

    @Column(name = "option_2", length = 500)
    private String option2;

    @Column(name = "option_3", length = 500)
    private String option3;

    @Column(name = "option_4", length = 500)
    private String option4;

    @Column(name = "answer_no")
    private Integer answerNo;

    @Column(name = "sample_answer", length = 1000)
    private String sampleAnswer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keyword_answer", columnDefinition = "jsonb")
    private String keywordAnswer;

    @Column(name = "rubric", length = 1000)
    private String rubric;

    @Column(name = "score")
    private Integer score;

    @Column(name = "explanation", length = 1000)
    private String explanation;

    public void updateQuestion(
            OnContentEntity content,
            String categoryName,
            QuestionType questionType,
            String questionText,
            String option1,
            String option2,
            String option3,
            String option4,
            Integer answerNo,
            String sampleAnswer,
            String keywordAnswer,
            String rubric,
            Integer score,
            String explanation
    ) {
        this.content = content;
        this.categoryName = categoryName;
        this.questionType = questionType;
        this.questionText = questionText;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.option4 = option4;
        this.answerNo = answerNo;
        this.sampleAnswer = sampleAnswer;
        this.keywordAnswer = keywordAnswer;
        this.rubric = rubric;
        this.score = score;
        this.explanation = explanation;
    }
}
