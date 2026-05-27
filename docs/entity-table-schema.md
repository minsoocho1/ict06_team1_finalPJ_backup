# Entity 기준 DB 테이블 구조

생성일: 2026-05-21

이 문서는 JPA 엔티티 애노테이션을 기준으로 DBeaver의 실제 테이블과 대조하기 위한 체크리스트입니다. DB 타입은 Java 타입과 @Column 속성으로 추정한 값이며, 실제 PostgreSQL/Hibernate 매핑과 세부 타입이 다를 수 있습니다.

- 포함: @Entity 클래스의 @Id, @Column, @JoinColumn, @Enumerated, BaseTimeEntity 상속 컬럼
- 제외: @OneToMany/@ManyToMany 컬렉션 필드 자체. @JoinTable 중간 테이블은 별도 섹션에 정리했습니다.
- 확인 포인트: 컬럼명, NULL 허용, 길이/precision/scale, PK/FK 컬럼 존재 여부, created_at/updated_at 존재 여부

## 테이블 목록

| No | Table | Entity | Columns | Source |
|---:|---|---|---:|---|
| 1 | AI_CHAT_MESSAGE | AiChatMessageEntity | 11 | src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiChatMessageEntity.java |
| 2 | AI_CHAT_SESSION | AiChatSessionEntity | 8 | src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiChatSessionEntity.java |
| 3 | AI_FEEDBACK | AiFeedbackEntity | 7 | src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiFeedbackEntity.java |
| 4 | AI_KNOWLEDGE_REQUEST | AiKnowledgeRequestEntity | 17 | src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiKnowledgeRequestEntity.java |
| 5 | AI_LOG | AiLogEntity | 11 | src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiLogEntity.java |
| 6 | AI_RETRIEVAL_TRACE | AiRetrievalTraceEntity | 9 | src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiRetrievalTraceEntity.java |
| 7 | AI_TEMPLATE | AiTemplateEntity | 15 | src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiTemplateEntity.java |
| 8 | AI_TEMPLATE_REQUEST | AiTemplateRequestEntity | 16 | src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiTemplateRequestEntity.java |
| 9 | APP_FILE | AppFileEntity | 5 | src/main/java/com/ict06/team1_fin_pj/domain/approval/entity/AppFileEntity.java |
| 10 | APP_FORM | AppFormEntity | 7 | src/main/java/com/ict06/team1_fin_pj/domain/approval/entity/AppFormEntity.java |
| 11 | APP_FORM | TestEntity | 7 | src/main/java/com/ict06/team1_fin_pj/test/entity/TestEntity.java |
| 12 | APP_LINE | AppLineEntity | 5 | src/main/java/com/ict06/team1_fin_pj/domain/approval/entity/AppLineEntity.java |
| 13 | APP_LINE_TEMPLATE | AppLineTemplateEntity | 6 | src/main/java/com/ict06/team1_fin_pj/domain/approval/entity/AppLineTemplateEntity.java |
| 14 | APP_LINE_TEMPLATE_DETAIL | AppLineTemplateDetailEntity | 10 | src/main/java/com/ict06/team1_fin_pj/domain/approval/entity/AppLineTemplateDetailEntity.java |
| 15 | APPROVAL | ApprovalEntity | 12 | src/main/java/com/ict06/team1_fin_pj/domain/approval/entity/ApprovalEntity.java |
| 16 | ATTENDANCE | AttendanceEntity | 13 | src/main/java/com/ict06/team1_fin_pj/domain/attendance/entity/AttendanceEntity.java |
| 17 | ATTENDANCE_CHANGE_LOG | AttChangeLogEntity | 12 | src/main/java/com/ict06/team1_fin_pj/domain/attendance/entity/AttChangeLogEntity.java |
| 18 | BUSINESS_TRIP | BusinessTripEntity | 12 | src/main/java/com/ict06/team1_fin_pj/domain/attendance/entity/BusinessTripEntity.java |
| 19 | CHECKLIST | ChecklistEntity | 10 | src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/ChecklistEntity.java |
| 20 | CHECKLIST_PROGRESS | ChecklistProgressEntity | 7 | src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/ChecklistProgressEntity.java |
| 21 | DEPARTMENT | DepartmentEntity | 3 | src/main/java/com/ict06/team1_fin_pj/domain/employee/entity/DepartmentEntity.java |
| 22 | DOC_CHUNKS | DocChunkEntity | 8 | src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/DocChunkEntity.java |
| 23 | DOC_VECTOR | DocVectorEntity | 6 | src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/DocVectorEntity.java |
| 24 | DOCUMENT | DocumentEntity | 11 | src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/DocumentEntity.java |
| 25 | DOCUMENT_PROCESS_LOG | DocumentProcessLogEntity | 11 | src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/DocumentProcessLogEntity.java |
| 26 | EMP_HISTORY | EmpHistoryEntity | 10 | src/main/java/com/ict06/team1_fin_pj/domain/employee/entity/EmpHistoryEntity.java |
| 27 | EMPLOYEE | EmpEntity | 20 | src/main/java/com/ict06/team1_fin_pj/domain/employee/entity/EmpEntity.java |
| 28 | EXT_CACHE | ExtCacheEntity | 4 | src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/ExtCacheEntity.java |
| 29 | GRADE_CODE | GradeCodeEntity | 6 | src/main/java/com/ict06/team1_fin_pj/domain/payroll/entity/GradeCodeEntity.java |
| 30 | LEARNING_SELF_CHECK | LearningSelfCheckEntity | 8 | src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/LearningSelfCheckEntity.java |
| 31 | LEAVE_OCCURRENCE | LeaveOccurrenceEntity | 12 | src/main/java/com/ict06/team1_fin_pj/domain/attendance/entity/LeaveOccurrenceEntity.java |
| 32 | LEAVE_REQUEST | LeaveRequestEntity | 12 | src/main/java/com/ict06/team1_fin_pj/domain/attendance/entity/LeaveRequestEntity.java |
| 33 | LEAVE_TYPE | LeaveTypeEntity | 8 | src/main/java/com/ict06/team1_fin_pj/domain/attendance/entity/LeaveTypeEntity.java |
| 34 | NOTIFICATION | NotificationEntity | 10 | src/main/java/com/ict06/team1_fin_pj/domain/notification/entity/NotificationEntity.java |
| 35 | ON_CONTENT | OnContentEntity | 13 | src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/OnContentEntity.java |
| 36 | PAY_ITEM_SETTING | PayItemSettingEntity | 9 | src/main/java/com/ict06/team1_fin_pj/domain/payroll/entity/PayItemSettingEntity.java |
| 37 | PAYROLL | PayrollEntity | 23 | src/main/java/com/ict06/team1_fin_pj/domain/payroll/entity/PayrollEntity.java |
| 38 | PAYROLL_ITEM | PayrollItemEntity | 13 | src/main/java/com/ict06/team1_fin_pj/domain/payroll/entity/PayrollItemEntity.java |
| 39 | POSITION | PositionEntity | 4 | src/main/java/com/ict06/team1_fin_pj/domain/employee/entity/PositionEntity.java |
| 40 | QUIZ_GENERATION_RULE | QuizGenerationRuleEntity | 10 | src/main/java/com/ict06/team1_fin_pj/domain/evaluation/entity/QuizGenerationRuleEntity.java |
| 41 | QUIZ_QUESTION | QuizQuestionEntity | 15 | src/main/java/com/ict06/team1_fin_pj/domain/evaluation/entity/QuizQuestionEntity.java |
| 42 | QUIZ_RESULT | QuizResultEntity | 11 | src/main/java/com/ict06/team1_fin_pj/domain/evaluation/entity/QuizResultEntity.java |
| 43 | ROAD_ITEM | RoadItemEntity | 9 | src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/RoadItemEntity.java |
| 44 | ROAD_PROGRESS | RoadProgressEntity | 5 | src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/RoadProgressEntity.java |
| 45 | ROADMAP | RoadmapEntity | 9 | src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/RoadmapEntity.java |
| 46 | ROLE | RoleEntity | 4 | src/main/java/com/ict06/team1_fin_pj/domain/employee/entity/RoleEntity.java |
| 47 | SALARY_POLICY | SalaryPolicyEntity | 11 | src/main/java/com/ict06/team1_fin_pj/domain/payroll/entity/SalaryPolicyEntity.java |
| 48 | SCH_PARTICIPANT | ScheduleParticipantEntity | 5 | src/main/java/com/ict06/team1_fin_pj/domain/calendar/entity/ScheduleParticipantEntity.java |
| 49 | SCHEDULE | ScheduleEntity | 16 | src/main/java/com/ict06/team1_fin_pj/domain/calendar/entity/ScheduleEntity.java |

## AI_CHAT_MESSAGE (AiChatMessageEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiChatMessageEntity.java`

Table constraints: `UNIQUE uk_session_seq (session_id, seq_no)`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| message_id | messageId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| session_id | session | AiChatSessionEntity | FK column | NOT NULL | ManyToOne -> AiChatSessionEntity |  |
| role | role | MessageRole | VARCHAR(20) | NOT NULL |  | Enum STRING |
| content | content | String | TEXT | NOT NULL |  | columnDefinition=TEXT |
| seq_no | seqNo | Integer | INTEGER | NOT NULL |  |  |
| model_name | modelName | String | VARCHAR(100) |  |  |  |
| prompt_tokens | promptTokens | Integer | INTEGER |  |  |  |
| completion_tokens | completionTokens | Integer | INTEGER |  |  |  |
| parent_message_id | parentMessage | AiChatMessageEntity | FK column |  | ManyToOne -> AiChatMessageEntity |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## AI_CHAT_SESSION (AiChatSessionEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiChatSessionEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| session_id | sessionId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| emp_no | employee | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| session_type | sessionType | SessionType | VARCHAR(20) | NOT NULL |  | Enum STRING |
| title | title | String | VARCHAR(200) |  |  |  |
| status | status | SessionStatus | VARCHAR(20) | NOT NULL |  | Enum STRING |
| last_message_at | lastMessageAt | LocalDateTime | TIMESTAMP |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## AI_FEEDBACK (AiFeedbackEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiFeedbackEntity.java`

Table constraints: `UNIQUE uk_message_emp (message_id, emp_no)`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| feedback_id | feedbackId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| message_id | message | AiChatMessageEntity | FK column | NOT NULL | ManyToOne -> AiChatMessageEntity |  |
| emp_no | employee | EmpEntity | FK column |  | ManyToOne -> EmpEntity |  |
| rating | rating | Integer | INTEGER | NOT NULL |  |  |
| reason | reason | String | TEXT |  |  | columnDefinition=TEXT |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## AI_KNOWLEDGE_REQUEST (AiKnowledgeRequestEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiKnowledgeRequestEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| knowledge_request_id | requestId | Long | BIGINT | PK, IDENTITY/Generated |  |  |
| requester_no | requester | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity (ref: emp_no) |  |
| title | title | String | VARCHAR(255) | NOT NULL |  |  |
| request_type | requestType | String | VARCHAR(50) | NOT NULL |  |  |
| category | category | String | VARCHAR(50) | NOT NULL |  |  |
| target_dept | targetDept | String | VARCHAR(100) |  |  |  |
| reason | reason | String | TEXT | NOT NULL |  | columnDefinition=TEXT |
| sample_question | sampleQuestion | String | TEXT | NOT NULL |  | columnDefinition=TEXT |
| reference_url | referenceUrl | String | VARCHAR(500) |  |  |  |
| access_level | accessLevel | String | VARCHAR(50) |  |  |  |
| status | status | AiKnowledgeStatus | VARCHAR(30) | NOT NULL |  | Enum STRING |
| admin_comment | adminComment | String | TEXT |  |  | columnDefinition=TEXT |
| reviewer_no | reviewer | EmpEntity | FK column |  | ManyToOne -> EmpEntity (ref: emp_no) |  |
| reviewed_at | reviewedAt | LocalDateTime | TIMESTAMP |  |  |  |
| target_doc_id | targetDoc | DocumentEntity | FK column |  | OneToOne -> DocumentEntity |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## AI_LOG (AiLogEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiLogEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| log_id | logId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| emp_no | employee | EmpEntity | FK column |  | ManyToOne -> EmpEntity |  |
| session_id | session | AiChatSessionEntity | FK column |  | ManyToOne -> AiChatSessionEntity |  |
| message_id | message | AiChatMessageEntity | FK column |  | ManyToOne -> AiChatMessageEntity |  |
| type | type | AiLogType | VARCHAR(50) | NOT NULL |  | Enum STRING |
| query | query | String | TEXT |  |  | columnDefinition=TEXT |
| response | response | String | TEXT |  |  | columnDefinition=TEXT |
| duration_ms | durationMs | Integer | INTEGER |  |  |  |
| error_message | errorMessage | String | TEXT |  |  | columnDefinition=TEXT |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## AI_RETRIEVAL_TRACE (AiRetrievalTraceEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiRetrievalTraceEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| trace_id | traceId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| log_id | log | AiLogEntity | FK column | NOT NULL | ManyToOne -> AiLogEntity |  |
| doc_id | document | DocumentEntity | FK column | NOT NULL | ManyToOne -> DocumentEntity |  |
| chunk_id | chunk | DocChunkEntity | FK column | NOT NULL | ManyToOne -> DocChunkEntity |  |
| similarity_score | similarityScore | BigDecimal | NUMERIC(6,4) |  |  |  |
| rerank_score | rerankScore | BigDecimal | NUMERIC(6,4) |  |  |  |
| used_in_answer_yn | usedInAnswer | Boolean | BOOLEAN |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## AI_TEMPLATE (AiTemplateEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiTemplateEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| template_id | templateId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| source_request_id | sourceRequest | AiTemplateRequestEntity | FK column |  | OneToOne -> AiTemplateRequestEntity |  |
| type | type | DocumentType | VARCHAR(20) | NOT NULL |  | Enum STRING |
| category | category | String | VARCHAR(100) |  |  |  |
| dept | dept | String | VARCHAR(100) |  |  |  |
| situation | situation | String | VARCHAR(255) |  |  |  |
| tone | tone | String | VARCHAR(50) |  |  |  |
| title | title | String | VARCHAR(255) | NOT NULL |  |  |
| description | description | String | TEXT |  |  | columnDefinition=TEXT |
| content | content | String | TEXT | NOT NULL |  | columnDefinition=TEXT |
| preview_json | previewJson | List<String> | jsonb |  |  | columnDefinition=jsonb |
| options_json | isActive | Boolean | jsonb |  |  | columnDefinition=jsonb |
| created_by | createdBy | EmpEntity | FK column |  | ManyToOne -> EmpEntity |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## AI_TEMPLATE_REQUEST (AiTemplateRequestEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/AiTemplateRequestEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| request_id | requestId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| emp_no | employee | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| type | type | DocumentType | VARCHAR(20) | NOT NULL |  | Enum STRING |
| category | category | String | VARCHAR(100) |  |  |  |
| dept | dept | String | VARCHAR(100) |  |  |  |
| situation | situation | String | VARCHAR(255) |  |  |  |
| tone | tone | String | VARCHAR(50) |  |  |  |
| title | title | String | VARCHAR(255) | NOT NULL |  |  |
| description | description | String | TEXT |  |  | columnDefinition=TEXT |
| content | content | String | TEXT | NOT NULL |  | columnDefinition=TEXT |
| preview_json | previewJson | List<String> | jsonb |  |  | columnDefinition=jsonb |
| options_json | status | RequestStatus | jsonb |  |  | Enum STRING; columnDefinition=jsonb |
| admin_comment | adminComment | String | TEXT |  |  | columnDefinition=TEXT |
| reviewed_at | reviewedAt | LocalDateTime | TIMESTAMP |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## APP_FILE (AppFileEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/approval/entity/AppFileEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| file_id | fileId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| approval_id | approval | ApprovalEntity | FK column |  | ManyToOne -> ApprovalEntity |  |
| file_name | fileName | String | VARCHAR(255) |  |  |  |
| file_path | filePath | String | VARCHAR(500) |  |  |  |
| file_size | fileSize | Long | BIGINT |  |  |  |

## APP_FORM (AppFormEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/approval/entity/AppFormEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| form_id | formId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| form_name | formName | String | VARCHAR(100) | NOT NULL |  |  |
| template | template | String | TEXT |  |  | columnDefinition=TEXT |
| line_template_id | lineTemplate | AppLineTemplateEntity | FK column |  | ManyToOne -> AppLineTemplateEntity |  |
| is_default | isDefault | Boolean | BOOLEAN | NOT NULL |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## APP_FORM (TestEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/test/entity/TestEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| form_id | formId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| form_name | formName | String | VARCHAR(100) | NOT NULL |  |  |
| template | template | String | TEXT |  |  | columnDefinition=TEXT |
| line_template_id | lineTemplate | AppLineTemplateEntity | FK column |  | ManyToOne -> AppLineTemplateEntity |  |
| is_default | isDefault | Boolean | BOOLEAN | NOT NULL |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## APP_LINE (AppLineEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/approval/entity/AppLineEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| line_id | lineId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| approval_id | approval | ApprovalEntity | FK column | NOT NULL | ManyToOne -> ApprovalEntity |  |
| approver_id | approver | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| step_order | stepOrder | Integer | INTEGER | NOT NULL |  |  |
| status | status | ApprovalLineStatus | VARCHAR(255) |  |  | Enum STRING |

## APP_LINE_TEMPLATE (AppLineTemplateEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/approval/entity/AppLineTemplateEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| template_id | templateId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| template_name | templateName | String | VARCHAR(100) | NOT NULL |  |  |
| is_default | isDefault | Boolean | BOOLEAN |  |  |  |
| created_by | createdBy | EmpEntity | FK column |  | ManyToOne -> EmpEntity |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## APP_LINE_TEMPLATE_DETAIL (AppLineTemplateDetailEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/approval/entity/AppLineTemplateDetailEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| detail_id | detailId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| template_id | template | AppLineTemplateEntity | FK column | NOT NULL | ManyToOne -> AppLineTemplateEntity |  |
| step_order | stepOrder | Integer | INTEGER | NOT NULL |  |  |
| approver_type | approverType | ApproverType | VARCHAR(20) | NOT NULL |  | Enum STRING |
| approver_id | approver | EmpEntity | FK column |  | ManyToOne -> EmpEntity |  |
| dept_id | department | DepartmentEntity | FK column |  | ManyToOne -> DepartmentEntity |  |
| min_position_id | minPosition | PositionEntity | FK column |  | ManyToOne -> PositionEntity |  |
| description | description | String | VARCHAR(200) |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## APPROVAL (ApprovalEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/approval/entity/ApprovalEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| approval_id | approvalId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| form_id | form | AppFormEntity | FK column |  | ManyToOne -> AppFormEntity |  |
| title | title | String | VARCHAR(200) |  |  |  |
| content | content | String | jsonb |  |  | columnDefinition=jsonb |
| writer_no | writer | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| current_step | currentStep | Integer | INTEGER |  |  |  |
| max_step | maxStep | Integer | INTEGER |  |  |  |
| current_approver_no | currentApprover | EmpEntity | FK column |  | ManyToOne -> EmpEntity |  |
| status | status | ApprovalStatus | VARCHAR(255) |  |  | Enum STRING |
| is_deleted | isDeleted | Boolean | BOOLEAN |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## ATTENDANCE (AttendanceEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/attendance/entity/AttendanceEntity.java`

Table constraints: `UNIQUE uk_emp_work_date (emp_no, work_date)`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| attendance_id | attendanceId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| emp_no | employee | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| work_date | workDate | LocalDate | DATE | NOT NULL |  |  |
| check_in_at | checkInAt | LocalDateTime | TIMESTAMP |  |  |  |
| check_out_at | checkOutAt | LocalDateTime | TIMESTAMP |  |  |  |
| check_in_lat | checkInLat | BigDecimal | NUMERIC(10,7) |  |  |  |
| check_in_long | checkInLong | BigDecimal | NUMERIC(10,7) |  |  |  |
| check_out_lat | checkOutLat | BigDecimal | NUMERIC(10,7) |  |  |  |
| check_out_long | checkOutLong | BigDecimal | NUMERIC(10,7) |  |  |  |
| work_hours | workHours | BigDecimal | NUMERIC(5,2) |  |  |  |
| overtime_mins | overtimeMins | Integer | INTEGER |  |  |  |
| status | status | AttendanceStatus | VARCHAR(20) | NOT NULL |  | Enum STRING |
| note | note | String | TEXT |  |  | columnDefinition=TEXT |

## ATTENDANCE_CHANGE_LOG (AttChangeLogEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/attendance/entity/AttChangeLogEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| change_log_id | changeLogId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| att_id | attendance | AttendanceEntity | FK column | NOT NULL | ManyToOne -> AttendanceEntity |  |
| work_date | workDate | LocalDate | DATE |  |  |  |
| before_check_in | beforeCheckIn | LocalDateTime | TIMESTAMP |  |  |  |
| after_check_in | afterCheckIn | LocalDateTime | TIMESTAMP |  |  |  |
| before_check_out | beforeCheckOut | LocalDateTime | TIMESTAMP |  |  |  |
| after_check_out | afterCheckOut | LocalDateTime | TIMESTAMP |  |  |  |
| before_status | beforeStatus | AttendanceStatus | VARCHAR(255) |  |  | Enum STRING |
| after_status | afterStatus | AttendanceStatus | VARCHAR(255) |  |  | Enum STRING |
| change_reason | changeReason | String | TEXT | NOT NULL |  | columnDefinition=TEXT |
| changed_by | changedBy | EmpEntity | FK column |  | ManyToOne -> EmpEntity |  |
| changed_at | changedAt | LocalDateTime | TIMESTAMP |  |  |  |

## BUSINESS_TRIP (BusinessTripEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/attendance/entity/BusinessTripEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| trip_id | tripId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| emp_no | employee | EmpEntity | FK column |  | ManyToOne -> EmpEntity |  |
| start_at | startAt | LocalDateTime | TIMESTAMP |  |  |  |
| end_at | endAt | LocalDateTime | TIMESTAMP |  |  |  |
| location_name | locationName | String | TEXT |  |  | columnDefinition=TEXT |
| content | content | String | TEXT |  |  | columnDefinition=TEXT |
| approval_id | approvalId | Integer | INTEGER |  |  |  |
| auth_lat | authLat | BigDecimal | NUMERIC(10,7) |  |  |  |
| auth_long | authLng | BigDecimal | NUMERIC(10,7) |  |  |  |
| is_verified | isVerified | Boolean | BOOLEAN |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## CHECKLIST (ChecklistEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/ChecklistEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| checklist_id | checklistId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| title | title | String | VARCHAR(200) | NOT NULL |  |  |
| category | category | String | VARCHAR(50) | NOT NULL |  |  |
| description | description | String | VARCHAR(500) |  |  |  |
| is_mandatory | isMandatory | Boolean | BOOLEAN |  |  |  |
| related_content_id | relatedContent | OnContentEntity | FK column |  | ManyToOne -> OnContentEntity |  |
| checklist_type | checklistType | ChecklistType | VARCHAR(20) | NOT NULL |  | Enum STRING |
| order_no | orderNo | Integer | INTEGER | NOT NULL |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## CHECKLIST_PROGRESS (ChecklistProgressEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/ChecklistProgressEntity.java`

Table constraints: `UNIQUE uk_emp_checklist (emp_no, checklist_id)`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| check_prog_id | checkProgId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| emp_no | employee | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| checklist_id | checklist | ChecklistEntity | FK column | NOT NULL | ManyToOne -> ChecklistEntity |  |
| status | status | ProgressStatus | VARCHAR(20) | NOT NULL |  | Enum STRING |
| completed_at | completedAt | LocalDateTime | TIMESTAMP |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## DEPARTMENT (DepartmentEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/employee/entity/DepartmentEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| dept_id | deptId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| dept_name | deptName | String | VARCHAR(50) | NOT NULL |  |  |
| parent_dept_id | parentDept | DepartmentEntity | FK column |  | ManyToOne -> DepartmentEntity |  |

## DOC_CHUNKS (DocChunkEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/DocChunkEntity.java`

Table constraints: `UNIQUE uk_doc_chunk_no (doc_id, chunk_no)`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| chunk_id | chunkId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| doc_id | document | DocumentEntity | FK column | NOT NULL | ManyToOne -> DocumentEntity |  |
| chunk_no | chunkNo | Integer | INTEGER |  |  |  |
| content | content | String | TEXT | NOT NULL |  | columnDefinition=TEXT |
| token_count | tokenCount | Integer | INTEGER |  |  |  |
| section_title | sectionTitle | String | VARCHAR(200) |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## DOC_VECTOR (DocVectorEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/DocVectorEntity.java`

Table constraints: `UNIQUE uk_chunk_vector (vector_id, chunk_id)`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| vector_id | vectorId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| chunk_id | chunk | DocChunkEntity | FK column | NOT NULL | OneToOne -> DocChunkEntity |  |
| embedding_data | embeddingData | String | TEXT |  |  | columnDefinition=TEXT |
| model_name | modelName | String | VARCHAR(100) | NOT NULL |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## DOCUMENT (DocumentEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/DocumentEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| doc_id | docId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| title | title | String | VARCHAR(255) | NOT NULL |  |  |
| file_path | filePath | String | VARCHAR(500) | NOT NULL |  |  |
| summary_preview | summaryPreview | String | TEXT |  |  | columnDefinition=TEXT |
| related_content_id | relatedContent | OnContentEntity | FK column |  | ManyToOne -> OnContentEntity |  |
| dept_id | department | DepartmentEntity | FK column |  | ManyToOne -> DepartmentEntity |  |
| access_level | accessLevel | AccessLevel | VARCHAR(30) | NOT NULL |  | Enum STRING |
| current_stage | currentStage | DocumentStage | VARCHAR(20) |  |  | Enum STRING |
| created_by | createdBy | EmpEntity | FK column |  | ManyToOne -> EmpEntity |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## DOCUMENT_PROCESS_LOG (DocumentProcessLogEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/DocumentProcessLogEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| job_id | jobId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| doc_id | document | DocumentEntity | FK column | NOT NULL | ManyToOne -> DocumentEntity |  |
| stage | stage | ProcessStage | VARCHAR(20) | NOT NULL |  | Enum STRING |
| status | status | ProcessStatus | VARCHAR(20) | NOT NULL |  | Enum STRING |
| started_at | startedAt | LocalDateTime | TIMESTAMP |  |  |  |
| ended_at | endedAt | LocalDateTime | TIMESTAMP |  |  |  |
| error_message | errorMessage | String | TEXT |  |  | columnDefinition=TEXT |
| retry_count | retryCount | Integer | INTEGER |  |  |  |
| processed_by | processedBy | EmpEntity | FK column |  | ManyToOne -> EmpEntity |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## EMP_HISTORY (EmpHistoryEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/employee/entity/EmpHistoryEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| history_id | historyId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| emp_no | employee | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| emp_id | empId | String | VARCHAR(20) | NOT NULL |  |  |
| old_dept_id | oldDept | DepartmentEntity | FK column |  | ManyToOne -> DepartmentEntity |  |
| new_dept_id | newDept | DepartmentEntity | FK column |  | ManyToOne -> DepartmentEntity |  |
| old_pos_id | oldPosition | PositionEntity | FK column |  | ManyToOne -> PositionEntity |  |
| new_pos_id | newPosition | PositionEntity | FK column |  | ManyToOne -> PositionEntity |  |
| change_type | changeType | String | VARCHAR(50) |  |  |  |
| changed_by | changedBy | EmpEntity | FK column |  | ManyToOne -> EmpEntity |  |
| changed_at | changedAt | LocalDateTime | TIMESTAMP |  |  |  |

## EMPLOYEE (EmpEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/employee/entity/EmpEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| emp_no | empNo | String | VARCHAR(20) | PK, NOT NULL, UNIQUE |  |  |
| emp_id | empId | String | VARCHAR(20) | NOT NULL, UNIQUE |  |  |
| password | password | String | VARCHAR(255) | NOT NULL |  |  |
| name | name | String | VARCHAR(50) | NOT NULL |  |  |
| email | email | String | VARCHAR(255) | UNIQUE |  |  |
| phone | phone | String | VARCHAR(255) | UNIQUE |  |  |
| bank | bank | String | VARCHAR(20) | NOT NULL |  |  |
| account_no | accountNo | String | VARCHAR(30) | NOT NULL, UNIQUE |  |  |
| dept_id | department | DepartmentEntity | FK column | NOT NULL | ManyToOne -> DepartmentEntity |  |
| position_id | position | PositionEntity | FK column | NOT NULL | ManyToOne -> PositionEntity |  |
| role_id | role | RoleEntity | FK column | NOT NULL | ManyToOne -> RoleEntity |  |
| grade_id | grade | GradeCodeEntity | FK column |  | ManyToOne -> GradeCodeEntity |  |
| hire_date | hireDate | LocalDate | DATE | NOT NULL |  |  |
| resignation_date | resignationDate | LocalDate | DATE |  |  |  |
| profile_img | profileImg | String | VARCHAR(255) |  |  |  |
| sign_img | signImg | String | VARCHAR(255) |  |  |  |
| is_deleted | isDeleted | String | char(1) |  |  | columnDefinition=char(1) |
| refresh_token | refreshToken | String | VARCHAR(255) |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## EXT_CACHE (ExtCacheEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/aiSecretary/entity/ExtCacheEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| cache_key | cacheKey | String | VARCHAR(100) | PK |  |  |
| cache_data | cacheData | String | jsonb | NOT NULL |  | columnDefinition=jsonb |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## GRADE_CODE (GradeCodeEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/payroll/entity/GradeCodeEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| grade_id | gradeId | String | VARCHAR(10) | PK |  |  |
| grade_name | gradeName | String | VARCHAR(50) | NOT NULL |  |  |
| description | description | String | VARCHAR(200) |  |  |  |
| is_active | isActive | Boolean | BOOLEAN |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## LEARNING_SELF_CHECK (LearningSelfCheckEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/LearningSelfCheckEntity.java`

Table constraints: `UNIQUE uk_learning_self_check_emp_content (emp_no, content_id)`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| self_check_id | selfCheckId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| emp_no | employee | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| content_id | content | OnContentEntity | FK column | NOT NULL | ManyToOne -> OnContentEntity |  |
| understanding_score | understandingScore | Integer | INTEGER | NOT NULL |  |  |
| confidence_score | confidenceScore | Integer | INTEGER | NOT NULL |  |  |
| need_more_explanation | needMoreExplanation | Boolean | BOOLEAN |  |  |  |
| memo | memo | String | VARCHAR(1000) |  |  |  |
| checked_at | checkedAt | LocalDateTime | TIMESTAMP |  |  |  |

## LEAVE_OCCURRENCE (LeaveOccurrenceEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/attendance/entity/LeaveOccurrenceEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| occurrence_id | occurrenceId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| emp_no | employee | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| type_id | leaveType | LeaveTypeEntity | FK column | NOT NULL | ManyToOne -> LeaveTypeEntity |  |
| target_year | targetYear | Integer | INTEGER | NOT NULL |  |  |
| occur_date | occurDate | LocalDate | DATE | NOT NULL |  |  |
| occur_days | occurDays | BigDecimal | NUMERIC(4,1) | NOT NULL |  |  |
| used_days | used_days | BigDecimal | NUMERIC(4,1) |  |  |  |
| remain_days | remain_days | BigDecimal | NUMERIC(4,1) |  |  |  |
| expiry_date | expiryDate | LocalDate | DATE |  |  |  |
| reason | reason | String | TEXT |  |  | columnDefinition=TEXT |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## LEAVE_REQUEST (LeaveRequestEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/attendance/entity/LeaveRequestEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| leave_request_id | leaveRequestId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| emp_no | employee | EmpEntity | FK column |  | ManyToOne -> EmpEntity |  |
| leave_type_id | leaveType | LeaveTypeEntity | FK column |  | ManyToOne -> LeaveTypeEntity |  |
| start_date | startDate | LocalDate | DATE | NOT NULL |  |  |
| end_date | endDate | LocalDate | DATE | NOT NULL |  |  |
| leave_days | leaveDays | BigDecimal | NUMERIC(4,1) |  |  |  |
| status | status | LeaveStatus | VARCHAR(255) |  |  | Enum STRING |
| approval_id | approval | ApprovalEntity | FK column |  | ManyToOne -> ApprovalEntity |  |
| reason | reason | String | TEXT |  |  | columnDefinition=TEXT |
| approved_at | approvedAt | LocalDateTime | TIMESTAMP |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## LEAVE_TYPE (LeaveTypeEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/attendance/entity/LeaveTypeEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| type_id | typeId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| type_name | typeName | String | VARCHAR(255) | NOT NULL |  |  |
| min_unit | minUnit | BigDecimal | NUMERIC(3,1) |  |  |  |
| is_paid | isPaid | Boolean | BOOLEAN |  |  |  |
| is_annual_deduct | isAnnualDeduct | Boolean | BOOLEAN |  |  |  |
| is_active | isActive | Boolean | BOOLEAN |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## NOTIFICATION (NotificationEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/notification/entity/NotificationEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| noti_id | notiId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| emp_no | employee | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| noti_type | notiType | NotificationType | VARCHAR(20) |  |  | Enum STRING |
| title | title | String | VARCHAR(100) |  |  |  |
| content | content | String | TEXT |  |  | columnDefinition=TEXT |
| url | url | String | VARCHAR(500) |  |  |  |
| is_read | isRead | Boolean | BOOLEAN |  |  |  |
| read_at | readAt | LocalDateTime | TIMESTAMP |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## ON_CONTENT (OnContentEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/OnContentEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| content_id | contentId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| title | title | String | VARCHAR(255) | NOT NULL |  |  |
| type | type | ContentType | VARCHAR(20) | NOT NULL |  | Enum STRING |
| category | category | String | VARCHAR(50) |  |  |  |
| sub_category | subCategory | String | VARCHAR(50) |  |  |  |
| target_position | targetPosition | String | VARCHAR(50) |  |  |  |
| difficulty | difficulty | Difficulty | VARCHAR(20) | NOT NULL |  | Enum STRING |
| estimated_time | estimatedTime | Integer | INTEGER |  |  |  |
| tags | tags | String | jsonb |  |  | columnDefinition=jsonb |
| is_mandatory | isMandatory | Boolean | BOOLEAN |  |  |  |
| path | path | String | VARCHAR(500) |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## PAY_ITEM_SETTING (PayItemSettingEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/payroll/entity/PayItemSettingEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| item_setting_id | itemSettingId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| item_name | itemName | String | VARCHAR(100) | NOT NULL |  |  |
| item_type | itemType | String | VARCHAR(20) | NOT NULL |  |  |
| non_tax_code | nonTaxCode | String | VARCHAR(20) |  |  |  |
| tax_type | taxType | String | VARCHAR(20) |  |  |  |
| linked_attendance_type | linkedAttendanceType | String | VARCHAR(30) |  |  |  |
| is_active | isActive | Boolean | BOOLEAN |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## PAYROLL (PayrollEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/payroll/entity/PayrollEntity.java`

Table constraints: `UNIQUE (emp_no, pay_month)`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| payroll_id | payrollId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| emp_no | employee | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| grade_id | grade | GradeCodeEntity | FK column |  | ManyToOne -> GradeCodeEntity |  |
| pay_month | payMonth | String | VARCHAR(7) | NOT NULL |  |  |
| family_count | familyCount | Integer | INTEGER |  |  |  |
| base_salary | baseSalary | BigDecimal | NUMERIC(15,2) |  |  |  |
| bonus | bonus | BigDecimal | NUMERIC(15,2) |  |  |  |
| total_allowance | totalAllowance | BigDecimal | NUMERIC(15,2) |  |  |  |
| total_gross | totalGross | BigDecimal | NUMERIC(15,2) |  |  |  |
| taxable_income | taxableIncome | BigDecimal | NUMERIC(15,2) |  |  |  |
| income_tax | incomeTax | BigDecimal | NUMERIC(15,2) |  |  |  |
| local_income_tax | localIncomeTax | BigDecimal | NUMERIC(15,2) |  |  |  |
| national_pension_amount | nationalPensionAmount | BigDecimal | NUMERIC(15,2) |  |  |  |
| health_insurance_amount | healthInsuranceAmount | BigDecimal | NUMERIC(15,2) |  |  |  |
| long_term_care_amount | longTermCareAmount | BigDecimal | NUMERIC(15,2) |  |  |  |
| employment_insurance_amount | employmentInsuranceAmount | BigDecimal | NUMERIC(15,2) |  |  |  |
| total_insurance | totalInsurance | BigDecimal | NUMERIC(15,2) |  |  |  |
| total_deduction | totalDeduction | BigDecimal | NUMERIC(15,2) |  |  |  |
| net_salary | netSalary | BigDecimal | NUMERIC(15,2) |  |  |  |
| status | status | PayrollStatus | VARCHAR(255) |  |  | Enum STRING |
| pay_date | payDate | LocalDate | DATE |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## PAYROLL_ITEM (PayrollItemEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/payroll/entity/PayrollItemEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| payroll_item_id | payrollItemId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| payroll_id | payroll | PayrollEntity | FK column | NOT NULL | ManyToOne -> PayrollEntity |  |
| item_setting_id | itemSetting | PayItemSettingEntity | FK column |  | ManyToOne -> PayItemSettingEntity |  |
| item_name_snapshot | itemNameSnapshot | String | VARCHAR(100) | NOT NULL |  |  |
| item_type | itemType | String | VARCHAR(20) | NOT NULL |  |  |
| amount | amount | BigDecimal | NUMERIC(15,2) | NOT NULL |  |  |
| tax_type | taxType | String | VARCHAR(20) |  |  |  |
| non_tax_code | nonTaxCode | String | VARCHAR(20) |  |  |  |
| taxable_amount | taxableAmount | BigDecimal | NUMERIC(15,2) |  |  |  |
| non_taxable_amount | nonTaxableAmount | BigDecimal | NUMERIC(15,2) |  |  |  |
| is_valid_non_tax | isValidNonTax | Boolean | BOOLEAN |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## POSITION (PositionEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/employee/entity/PositionEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| position_id | positionId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| position_name | positionName | String | VARCHAR(50) | NOT NULL |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## QUIZ_GENERATION_RULE (QuizGenerationRuleEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/evaluation/entity/QuizGenerationRuleEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| rule_id | ruleId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| category_name | categoryName | String | VARCHAR(50) | NOT NULL |  |  |
| question_count | questionCount | Integer | INTEGER | NOT NULL |  |  |
| pass_score | passScore | Integer | INTEGER | NOT NULL |  |  |
| weight_percent | weightPercent | Integer | INTEGER | NOT NULL |  |  |
| difficulty | difficulty | Difficulty | VARCHAR(20) | NOT NULL |  | Enum STRING |
| question_type | questionType | QuestionType | VARCHAR(20) | NOT NULL |  | Enum STRING |
| is_active | isActive | Boolean | BOOLEAN |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## QUIZ_QUESTION (QuizQuestionEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/evaluation/entity/QuizQuestionEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| question_id | questionId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| content_id | content | OnContentEntity | FK column |  | ManyToOne -> OnContentEntity |  |
| category_name | categoryName | String | VARCHAR(50) | NOT NULL |  |  |
| question_type | questionType | QuestionType | VARCHAR(20) | NOT NULL |  | Enum STRING |
| question_text | questionText | String | VARCHAR(1000) | NOT NULL |  |  |
| option_1 | option1 | String | VARCHAR(500) |  |  |  |
| option_2 | option2 | String | VARCHAR(500) |  |  |  |
| option_3 | option3 | String | VARCHAR(500) |  |  |  |
| option_4 | option4 | String | VARCHAR(500) |  |  |  |
| answer_no | answerNo | Integer | INTEGER |  |  |  |
| sample_answer | sampleAnswer | String | VARCHAR(1000) |  |  |  |
| keyword_answer | keywordAnswer | String | jsonb |  |  | columnDefinition=jsonb |
| rubric | rubric | String | VARCHAR(1000) |  |  |  |
| score | score | Integer | INTEGER |  |  |  |
| explanation | explanation | String | VARCHAR(1000) |  |  |  |

## QUIZ_RESULT (QuizResultEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/evaluation/entity/QuizResultEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| result_id | resultId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| emp_no | employee | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| question_id | question | QuizQuestionEntity | FK column | NOT NULL | ManyToOne -> QuizQuestionEntity |  |
| selected_no | selectedNo | Integer | INTEGER |  |  |  |
| answer_text | answerText | String | VARCHAR(2000) |  |  |  |
| is_correct | isCorrect | Boolean | BOOLEAN |  |  |  |
| score | score | Integer | INTEGER |  |  |  |
| ai_score | aiScore | BigDecimal | NUMERIC(5,2) |  |  |  |
| ai_feedback | aiFeedback | String | VARCHAR(2000) |  |  |  |
| similarity_score | similarityScore | BigDecimal | NUMERIC(5,2) |  |  |  |
| submitted_at | submittedAt | LocalDateTime | TIMESTAMP |  |  |  |

## ROAD_ITEM (RoadItemEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/RoadItemEntity.java`

Table constraints: `INDEX idx_roadmap_order (roadmap_id, order_no)`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| item_id | itemId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| item_title | itemTitle | String | VARCHAR(200) |  |  |  |
| recommendation_reason | recommendationReason | String | VARCHAR(300) |  |  |  |
| roadmap_id | roadmap | RoadmapEntity | FK column | NOT NULL | ManyToOne -> RoadmapEntity |  |
| content_id | content | OnContentEntity | FK column | NOT NULL | ManyToOne -> OnContentEntity |  |
| category_name | categoryName | String | VARCHAR(50) | NOT NULL |  |  |
| order_no | orderNo | Integer | INTEGER | NOT NULL |  |  |
| start_date | startDate | LocalDate | DATE |  |  |  |
| due_date | dueDate | LocalDate | DATE |  |  |  |

## ROAD_PROGRESS (RoadProgressEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/RoadProgressEntity.java`

Table constraints: `UNIQUE uk_emp_item (emp_no, item_id)`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| road_prog_id | roadProgId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| emp_no | employee | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| item_id | item | RoadItemEntity | FK column | NOT NULL | ManyToOne -> RoadItemEntity |  |
| status | status | ProgressStatus | VARCHAR(20) |  |  | Enum STRING |
| rate | rate | BigDecimal | NUMERIC(5,2) |  |  |  |

## ROADMAP (RoadmapEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/RoadmapEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| roadmap_id | roadmapId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| title | title | String | VARCHAR(200) | NOT NULL |  |  |
| emp_no | employee | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| dept_id | department | DepartmentEntity | FK column |  | ManyToOne -> DepartmentEntity |  |
| position_id | position | PositionEntity | FK column |  | ManyToOne -> PositionEntity |  |
| generated_type | generatedType | GeneratedType | VARCHAR(20) |  |  | Enum STRING |
| is_completed | isCompleted | Boolean | BOOLEAN |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## ROLE (RoleEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/employee/entity/RoleEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| role_id | roleId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| role_name | roleName | String | VARCHAR(50) | NOT NULL |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## SALARY_POLICY (SalaryPolicyEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/payroll/entity/SalaryPolicyEntity.java`

Table constraints: `UNIQUE (grade_id, dept_id, position_id)`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| policy_id | policyId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| grade_id | grade | GradeCodeEntity | FK column | NOT NULL | ManyToOne -> GradeCodeEntity |  |
| dept_id | department | DepartmentEntity | FK column | NOT NULL | ManyToOne -> DepartmentEntity |  |
| position_id | position | PositionEntity | FK column | NOT NULL | ManyToOne -> PositionEntity |  |
| basic_salary | basicSalary | BigDecimal | NUMERIC(15,2) | NOT NULL |  |  |
| bonus_rate | bonusRate | BigDecimal | NUMERIC(5,2) | NOT NULL |  |  |
| position_allowance | positionAllowance | BigDecimal | NUMERIC(15,2) | NOT NULL |  |  |
| description | description | String | VARCHAR(200) |  |  |  |
| is_active | isActive | Boolean | BOOLEAN |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## SCH_PARTICIPANT (ScheduleParticipantEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/calendar/entity/ScheduleParticipantEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| sch_parti_id | schPartiId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| schedule_id | schedule | ScheduleEntity | FK column | NOT NULL | ManyToOne -> ScheduleEntity |  |
| emp_no | employee | EmpEntity | FK column | NOT NULL | ManyToOne -> EmpEntity |  |
| status | status | ParticipantStatus | VARCHAR(255) |  |  | Enum STRING |
| responded_at | respondedAt | LocalDateTime | TIMESTAMP |  |  |  |

## SCHEDULE (ScheduleEntity)

Source: `src/main/java/com/ict06/team1_fin_pj/domain/calendar/entity/ScheduleEntity.java`

| 컬럼명 | 필드명 | Java 타입 | DB 타입/정의 | 제약 | 관계 | 비고 |
|---|---|---|---|---|---|---|
| schedule_id | scheduleId | Integer | INTEGER | PK, IDENTITY/Generated |  |  |
| title | title | String | VARCHAR(200) | NOT NULL |  |  |
| content | content | String | TEXT |  |  | columnDefinition=TEXT |
| start_time | startTime | LocalDateTime | TIMESTAMP |  |  |  |
| end_time | endTime | LocalDateTime | TIMESTAMP |  |  |  |
| type | type | ScheduleType | VARCHAR(255) |  |  | Enum STRING |
| creator_no | creator | EmpEntity | FK column |  | ManyToOne -> EmpEntity |  |
| dept_id | department | DepartmentEntity | FK column |  | ManyToOne -> DepartmentEntity |  |
| category | category | String | VARCHAR(20) |  |  |  |
| location | location | String | VARCHAR(200) |  |  |  |
| is_all_day | isAllDay | Boolean | BOOLEAN |  |  |  |
| is_public | isPublic | Boolean | BOOLEAN |  |  |  |
| repeat_rule | repeatRule | String | VARCHAR(50) |  |  |  |
| is_deleted | isDeleted | Boolean | BOOLEAN |  |  |  |
| created_at | createdAt | LocalDateTime | TIMESTAMP | updatable=false | BaseTimeEntity | @CreatedDate |
| updated_at | updatedAt | LocalDateTime | TIMESTAMP |  | BaseTimeEntity | @LastModifiedDate |

## @JoinTable 중간 테이블

| 중간 테이블 | 소유 엔티티 | 필드 | Java 타입 | Join 컬럼 | Inverse 컬럼 | Source |
|---|---|---|---|---|---|---|
| DOCUMENT_ON_CONTENT | DocumentEntity | relatedContents | Set<OnContentEntity> | doc_id | content_id | src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/DocumentEntity.java |
| ON_CONTENT_TARGET_DEPARTMENT | OnContentEntity | targetDepartments | Set<DepartmentEntity> | content_id | dept_id | src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/OnContentEntity.java |
| ON_CONTENT_TARGET_POSITION | OnContentEntity | targetPositions | Set<PositionEntity> | content_id | position_id | src/main/java/com/ict06/team1_fin_pj/domain/onboarding/entity/OnContentEntity.java |
