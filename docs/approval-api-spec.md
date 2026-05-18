# 전자결재 API 명세서

## 1. 공통 정보

### Base URL

```text
/api/approval
```

### 인증

- 모든 API는 로그인 사용자의 JWT 인증 정보를 사용합니다.
- 백엔드에서는 `@AuthenticationPrincipal PrincipalDetails principal`에서 로그인 사용자의 사번(`empNo`)을 가져옵니다.
- 작성자 사번, 결재자 사번 같은 권한 판단에 필요한 값은 클라이언트가 임의로 보내지 않고, 서버에서 JWT와 DB 데이터를 기준으로 검증합니다.

### 페이징

목록 API는 Spring Data `Page` 형식으로 응답합니다.

요청 파라미터 예시:

```text
?page=0&size=10&sort=updatedAt,desc
```

기본값:

```text
size=10
sort=updatedAt,DESC
```

응답 구조 예시:

```json
{
  "content": [],
  "pageable": {},
  "totalElements": 0,
  "totalPages": 0,
  "last": true,
  "size": 10,
  "number": 0,
  "first": true,
  "numberOfElements": 0,
  "empty": true
}
```

### 결재 문서 상태

| 값 | 라벨 | 의미 |
| --- | --- | --- |
| `DRAFT` | 임시저장 | 아직 상신하지 않은 작성 중 문서 |
| `PENDING` | 대기 | 현재 코드에서는 기본 enum 값으로만 존재 |
| `IN_PROGRESS` | 진행 | 상신되어 결재가 진행 중인 문서 |
| `COMPLETED` | 완료 | 모든 결재자가 승인한 문서 |
| `REJECTED` | 반려 | 결재자 중 한 명이 반려한 문서 |
| `CANCELED` | 취소 | 작성자가 상신 취소한 문서 |

### 결재선 상태

| 값 | 라벨 | 의미 |
| --- | --- | --- |
| `WAITING` | 대기 | 아직 처리하지 않은 결재선 |
| `APPROVED` | 승인 | 결재자가 승인한 결재선 |
| `REJECTED` | 반려 | 결재자가 반려한 결재선 |

### 참조자 규칙

- `approvalLines[].stepOrder = 0`이면 참조자입니다.
- 참조자는 결재자가 아니므로 승인/반려할 수 없습니다.
- 참조자는 상신 이후 문서를 열람할 수 있습니다.
- 임시저장(`DRAFT`) 문서는 아직 공유된 문서가 아니므로 참조자에게 노출하지 않습니다.

## 2. 공통 DTO

### ApprovalCreateRequestDto

신규 임시저장, 신규 상신, 임시저장 수정, 임시저장 상신에서 공통으로 사용합니다.

```json
{
  "formId": 1,
  "title": "연차 신청서",
  "content": "{\"startDate\":\"2026-05-20\",\"endDate\":\"2026-05-20\",\"reason\":\"개인 사유\"}",
  "approvalLines": [
    {
      "approverNo": "20240001",
      "stepOrder": 1
    },
    {
      "approverNo": "20240002",
      "stepOrder": 2
    },
    {
      "approverNo": "20240003",
      "stepOrder": 0
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `formId` | number | Y | 결재 양식 ID |
| `title` | string | Y | 결재 문서 제목 |
| `content` | string | N | 양식별 상세 입력값 JSON 문자열 |
| `approvalLines` | array | 상신 시 Y | 결재자/참조자 목록 |
| `approvalLines[].approverNo` | string | Y | 결재자 또는 참조자 사번 |
| `approvalLines[].stepOrder` | number | Y | 결재 순서. `0`은 참조자, `1` 이상은 결재자 |

### ApprovalCreateResponseDto

저장, 상신, 승인, 반려, 취소 후 공통 응답입니다.

```json
{
  "approvalId": 10,
  "status": "IN_PROGRESS",
  "currentStep": 1,
  "maxStep": 2,
  "currentApproverNo": "20240001",
  "currentApproverName": "홍길동"
}
```

### ApprovalListResponseDto

문서함 목록 응답의 `content[]` 항목입니다.

```json
{
  "approvalId": 10,
  "formId": 1,
  "formName": "연차 신청서",
  "writerNo": "20240010",
  "writerName": "김작성",
  "title": "연차 신청서",
  "status": "IN_PROGRESS",
  "statusLabel": "진행",
  "currentStep": 1,
  "maxStep": 2,
  "currentApproverNo": "20240001",
  "currentApproverName": "홍길동",
  "createdAt": "2026-05-12T10:00:00",
  "updatedAt": "2026-05-12T10:00:00"
}
```

### ApprovalDetailResponseDto

문서 상세 조회 응답입니다.

```json
{
  "approvalId": 10,
  "formId": 1,
  "formName": "연차 신청서",
  "title": "연차 신청서",
  "content": "{\"startDate\":\"2026-05-20\"}",
  "status": "IN_PROGRESS",
  "statusLabel": "진행",
  "writerNo": "20240010",
  "writerName": "김작성",
  "currentStep": 1,
  "maxStep": 2,
  "currentApproverNo": "20240001",
  "currentApproverName": "홍길동",
  "createdAt": "2026-05-12T10:00:00",
  "updatedAt": "2026-05-12T10:00:00",
  "lines": [
    {
      "lineId": 1,
      "approverNo": "20240001",
      "approverName": "홍길동",
      "stepOrder": 1,
      "reference": false,
      "status": "WAITING",
      "statusLabel": "대기",
      "processedAt": null
    }
  ],
  "files": [
    {
      "fileId": 1,
      "fileName": "receipt.png",
      "filePath": "/approval/uploads/uuid.png",
      "fileSize": 12345
    }
  ]
}
```

## 3. 문서 작성 API

### 3.1 새 문서 임시저장(JSON)

```http
POST /api/approval/drafts
Content-Type: application/json
```

요청:

```json
{
  "formId": 1,
  "title": "연차 신청서",
  "content": "{\"startDate\":\"2026-05-20\"}",
  "approvalLines": [
    {
      "approverNo": "20240001",
      "stepOrder": 1
    }
  ]
}
```

응답: `ApprovalCreateResponseDto`

권한/상태:

- 로그인 사용자만 호출 가능합니다.
- 작성자는 JWT의 로그인 사용자로 저장됩니다.
- 결과 상태는 `DRAFT`입니다.

### 3.2 새 문서 임시저장(multipart)

```http
POST /api/approval/drafts
Content-Type: multipart/form-data
```

파트:

| 파트명 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `request` | JSON | Y | `ApprovalCreateRequestDto` |
| `files` | File[] | N | 첨부파일 목록 |

JavaScript 예시:

```javascript
const formData = new FormData();
formData.append(
  "request",
  new Blob([JSON.stringify(requestBody)], { type: "application/json" })
);
files.forEach((file) => formData.append("files", file));
```

응답: `ApprovalCreateResponseDto`

### 3.3 새 문서 상신(JSON)

```http
POST /api/approval/submit
Content-Type: application/json
```

요청: `ApprovalCreateRequestDto`

응답: `ApprovalCreateResponseDto`

권한/상태:

- 결재선에 `stepOrder > 0`인 실제 결재자가 1명 이상 필요합니다.
- `stepOrder = 0`인 참조자는 결재 단계 계산에서 제외됩니다.
- 결과 상태는 `IN_PROGRESS`입니다.
- 첫 번째 결재자가 `currentApprover`가 됩니다.

### 3.4 새 문서 상신(multipart)

```http
POST /api/approval/submit
Content-Type: multipart/form-data
```

파트:

| 파트명 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `request` | JSON | Y | `ApprovalCreateRequestDto` |
| `files` | File[] | N | 첨부파일 목록 |

응답: `ApprovalCreateResponseDto`

## 4. 임시저장 문서 API

### 4.1 임시저장 문서함 목록

```http
GET /api/approval/drafts?page=0&size=10
```

응답: `Page<ApprovalListResponseDto>`

권한/상태:

- 로그인 사용자가 작성한 `DRAFT` 문서만 조회합니다.

### 4.2 기존 임시저장 문서 수정(JSON)

```http
PUT /api/approval/drafts/{approvalId}
Content-Type: application/json
```

요청: `ApprovalCreateRequestDto`

응답: `ApprovalCreateResponseDto`

권한/상태:

- 작성자 본인의 `DRAFT` 문서만 수정할 수 있습니다.
- 제목, 본문, 양식, 결재선을 요청 값으로 갱신합니다.

### 4.3 기존 임시저장 문서 수정(multipart)

```http
PUT /api/approval/drafts/{approvalId}
Content-Type: multipart/form-data
```

파트:

| 파트명 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `request` | JSON | Y | `ApprovalCreateRequestDto` |
| `files` | File[] | N | 추가 첨부파일 목록 |

주의:

- 기존 파일 삭제/교체는 이 API에서 직접 처리하지 않습니다.
- 기존 파일은 `DELETE /api/approval/files/{fileId}`로 삭제하고, 새 파일은 이 multipart API로 추가합니다.

### 4.4 기존 임시저장 문서 상신(JSON)

```http
POST /api/approval/drafts/{approvalId}/submit
Content-Type: application/json
```

요청: `ApprovalCreateRequestDto`

응답: `ApprovalCreateResponseDto`

권한/상태:

- 작성자 본인의 `DRAFT` 문서만 상신할 수 있습니다.
- 실제 결재자가 1명 이상 필요합니다.
- 상신 후 상태는 `IN_PROGRESS`입니다.

### 4.5 기존 임시저장 문서 상신(multipart)

```http
POST /api/approval/drafts/{approvalId}/submit
Content-Type: multipart/form-data
```

파트:

| 파트명 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `request` | JSON | Y | `ApprovalCreateRequestDto` |
| `files` | File[] | N | 추가 첨부파일 목록 |

응답: `ApprovalCreateResponseDto`

### 4.6 첨부파일 삭제

```http
DELETE /api/approval/files/{fileId}
```

응답:

```text
204 또는 200 계열 응답 바디 없음
```

권한/상태:

- 작성자 본인의 `DRAFT` 문서에 속한 파일만 삭제할 수 있습니다.
- DB의 `APP_FILE` 행과 서버의 실제 파일을 함께 삭제합니다.

## 5. 개인 문서함 API

### 5.1 개인 문서함 목록

```http
GET /api/approval/my-documents
GET /api/approval/my-documents?status=IN_PROGRESS
```

쿼리 파라미터:

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `status` | string | N | `IN_PROGRESS`, `COMPLETED`, `REJECTED`, `CANCELED` 등 |
| `page` | number | N | 페이지 번호 |
| `size` | number | N | 페이지 크기 |
| `sort` | string | N | 정렬 조건 |

응답: `Page<ApprovalListResponseDto>`

권한/상태:

- 로그인 사용자가 작성한 문서를 조회합니다.
- `status`가 없으면 `DRAFT`를 제외한 문서를 조회합니다.
- `DRAFT` 문서는 임시저장 문서함 API에서 조회합니다.

### 5.2 참조 문서함 목록

```http
GET /api/approval/referenced-documents
GET /api/approval/referenced-documents?status=IN_PROGRESS
```

응답: `Page<ApprovalListResponseDto>`

권한/상태:

- 로그인 사용자가 참조자(`stepOrder=0`)로 포함된 문서를 조회합니다.
- `DRAFT` 문서는 항상 제외합니다.
- 참조자는 열람만 가능하고 승인/반려는 불가합니다.

### 5.3 문서 상세 조회

```http
GET /api/approval/{approvalId}
```

응답: `ApprovalDetailResponseDto`

권한/상태:

- 작성자는 자신의 문서를 조회할 수 있습니다.
- 결재선에 포함된 결재자와 참조자는 상신 이후 문서를 조회할 수 있습니다.
- `DRAFT` 문서는 작성자만 조회할 수 있습니다.

### 5.4 상신 취소

```http
POST /api/approval/{approvalId}/cancel
```

응답: `ApprovalCreateResponseDto`

권한/상태:

- 작성자 본인만 취소할 수 있습니다.
- `IN_PROGRESS` 문서만 취소할 수 있습니다.
- 아직 어떤 결재자도 `APPROVED` 또는 `REJECTED` 처리하지 않은 문서만 취소할 수 있습니다.
- 취소 후 문서 상태는 `CANCELED`입니다.

## 6. 결재자 전용 API

### 6.1 결재 대기 문서함 목록

```http
GET /api/approval/pending-documents
```

응답: `Page<ApprovalListResponseDto>`

조회 조건:

- 문서 상태가 `IN_PROGRESS`
- `currentApprover`가 로그인 사용자

사용 화면:

- “지금 내가 승인/반려해야 하는 문서” 목록

### 6.2 결재 예정 문서함 목록

```http
GET /api/approval/upcoming-documents
```

응답: `Page<ApprovalListResponseDto>`

조회 조건:

- 문서 상태가 `IN_PROGRESS`
- 로그인 사용자가 결재선에 포함되어 있음
- `stepOrder > 0`
- `stepOrder > currentStep`

사용 화면:

- “결재선에는 포함되어 있지만 아직 내 차례가 아닌 문서” 목록

### 6.3 결재 승인

```http
POST /api/approval/{approvalId}/approve
```

응답: `ApprovalCreateResponseDto`

권한/상태:

- 현재 결재자(`currentApprover`)가 로그인 사용자일 때만 승인할 수 있습니다.
- 문서 상태가 `IN_PROGRESS`일 때만 승인할 수 있습니다.
- 현재 결재선 상태가 `WAITING`일 때만 승인할 수 있습니다.

상태 변화:

- 현재 결재선: `WAITING` -> `APPROVED`
- 다음 결재자가 있으면:
  - 문서 상태 유지: `IN_PROGRESS`
  - `currentStep`, `currentApprover`를 다음 결재자로 이동
- 다음 결재자가 없으면:
  - 문서 상태: `COMPLETED`
  - `currentApprover`: `null`

### 6.4 결재 반려

```http
POST /api/approval/{approvalId}/reject
```

응답: `ApprovalCreateResponseDto`

권한/상태:

- 현재 결재자(`currentApprover`)가 로그인 사용자일 때만 반려할 수 있습니다.
- 문서 상태가 `IN_PROGRESS`일 때만 반려할 수 있습니다.
- 현재 결재선 상태가 `WAITING`일 때만 반려할 수 있습니다.

상태 변화:

- 현재 결재선: `WAITING` -> `REJECTED`
- 문서 상태: `REJECTED`
- `currentApprover`: `null`

## 7. 프론트 구현 참고

### 메뉴 노출 기준

결재자 메뉴는 다음 API 중 하나라도 결과가 있으면 노출하는 방식으로 구현할 수 있습니다.

- `GET /api/approval/pending-documents`
- `GET /api/approval/upcoming-documents`

또는 사용자 정보에서 직급/권한을 판단하는 별도 정책이 있다면 그 정책과 함께 사용할 수 있습니다.

### 버튼 노출 기준

| 버튼 | 노출 조건 |
| --- | --- |
| 임시저장 수정 | 상세 문서가 `DRAFT`이고 작성자 본인 |
| 임시저장 상신 | 상세 문서가 `DRAFT`이고 작성자 본인 |
| 첨부파일 삭제 | 상세 문서가 `DRAFT`이고 작성자 본인 |
| 상신 취소 | 상세 문서가 `IN_PROGRESS`, 작성자 본인, 아직 결재 처리 전 |
| 승인 | 상세 문서가 `IN_PROGRESS`이고 `currentApproverNo`가 로그인 사용자 사번 |
| 반려 | 상세 문서가 `IN_PROGRESS`이고 `currentApproverNo`가 로그인 사용자 사번 |

### 상태별 문서함 권장 매핑

| 화면 | API |
| --- | --- |
| 개인 문서함 전체 | `GET /api/approval/my-documents` |
| 개인 문서함 진행중 | `GET /api/approval/my-documents?status=IN_PROGRESS` |
| 개인 문서함 완료 | `GET /api/approval/my-documents?status=COMPLETED` |
| 개인 문서함 반려 | `GET /api/approval/my-documents?status=REJECTED` |
| 개인 문서함 취소 | `GET /api/approval/my-documents?status=CANCELED` |
| 임시저장함 | `GET /api/approval/drafts` |
| 참조 문서함 | `GET /api/approval/referenced-documents` |
| 결재 대기 문서함 | `GET /api/approval/pending-documents` |
| 결재 예정 문서함 | `GET /api/approval/upcoming-documents` |

