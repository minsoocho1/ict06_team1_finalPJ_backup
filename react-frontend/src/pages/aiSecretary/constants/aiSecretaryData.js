/**
 * @FileName : aiSecretaryData.js
 * @Description : AI 비서 공용 데이터
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.30  송혜진       최초 생성
 * @ 2026.05.12  송혜진       templateCards 내 dept 추가
 */

export const docMeta = {
  REPORT: {
    label: "보고서 초안",
    badge: "REPORT",
    description: "업무 결과와 진행 현황을 정리하는 문서입니다.",
  },
  MINUTES: {
    label: "회의록 정리",
    badge: "MINUTES",
    description: "회의 내용을 구조화해 결정 사항과 후속 작업을 남깁니다.",
  },
  APPROVAL: {
    label: "결재 사유",
    badge: "APPROVAL",
    description: "결재 요청의 배경과 필요성을 정리합니다.",
  },
  correction: {
    label: "문장 다듬기",
    badge: "CORRECTION",
    description: "맞춤법과 표현을 자연스럽게 정리합니다.",
  },
  template: {
    label: "템플릿 생성",
    badge: "TEMPLATE",
    description: "반복 문서를 빠르게 시작할 수 있는 템플릿을 만듭니다.",
  },
};

export const recentDocsSeed = [
  {
    id: "r1",
    title: "3분기 마케팅 성과 보고서 초안",
    date: "2024.05.24",
    screen: "writer",
    type: "REPORT",
  },
  {
    id: "r2",
    title: "신규 프로젝트 추진 계획",
    date: "2024.05.22",
    screen: "form",
    type: "REPORT",
  },
  {
    id: "r3",
    title: "4월 중간 회의록",
    date: "2024.05.21",
    screen: "form",
    type: "MINUTES",
  },
  {
    id: "r4",
    title: "외부 교육 참가 결재 요청",
    date: "2024.05.20",
    screen: "form",
    type: "APPROVAL",
  },
  {
    id: "r5",
    title: "프로젝트 주간 보고 초안",
    date: "2024.05.19",
    screen: "writer",
    type: "REPORT",
  },
  {
    id: "r6",
    title: "기안문 문장 다듬기",
    date: "2024.05.18",
    screen: "writer",
    type: "REPORT",
  },
  {
    id: "r7",
    title: "인사팀 보고서 초안",
    date: "2024.05.17",
    screen: "form",
    type: "REPORT",
  },
  {
    id: "r8",
    title: "5월 주간 회의록",
    date: "2024.05.16",
    screen: "form",
    type: "MINUTES",
  },
  {
    id: "r9",
    title: "예산 집행 승인 요청",
    date: "2024.05.15",
    screen: "form",
    type: "APPROVAL",
  },
  {
    id: "r10",
    title: "교육 참가 안내문 초안",
    date: "2024.05.14",
    screen: "form",
    type: "REPORT",
  },
  {
    id: "r11",
    title: "주간 업무 보고 초안",
    date: "2024.05.13",
    screen: "writer",
    type: "REPORT",
  },
  {
    id: "r12",
    title: "출장 진행 요청 결재",
    date: "2024.05.12",
    screen: "form",
    type: "APPROVAL",
  },
];

export const templateCards = [
  {
    id: "tpl-report-weekly-dev",
    type: "REPORT",
    category: "보고",
    dept: "개발1팀(BE)",
    situation: "주간 업무 공유",
    title: "주간 업무 보고서",
    desc: "한 주 동안 진행한 업무와 이슈를 빠르게 공유하는 보고서 템플릿입니다.",
    preview: [
      "1. 이번 주 진행 내용",
      "2. 주요 이슈",
      "3. 다음 주 계획",
      "4. 요청 사항",
    ],
  },
  {
    id: "tpl-report-project-status",
    type: "REPORT",
    category: "보고",
    dept: "개발2팀(FE)",
    situation: "프로젝트 진행 현황",
    title: "프로젝트 진행 보고서",
    desc: "프로젝트 진행 상태와 주요 이슈를 정리하는 보고서 템플릿입니다.",
    preview: [
      "1. 프로젝트 개요",
      "2. 현재 진행 현황",
      "3. 주요 이슈",
      "4. 향후 계획",
    ],
  },
  {
    id: "tpl-report-team-share",
    type: "REPORT",
    category: "보고",
    dept: "경영지원팀, 인사팀",
    situation: "회의 결과 공유",
    title: "회의 결과 공유 템플릿",
    desc: "회의 주요 안건, 결정 사항, 후속 작업을 빠르게 공유하는 템플릿입니다.",
    preview: [
      "1. 회의 개요",
      "2. 주요 논의 내용",
      "3. 결정 사항",
      "4. 후속 작업",
    ],
  },
  {
    id: "tpl-minutes-weekly-meeting",
    type: "MINUTES",
    category: "회의록",
    dept: "경영지원팀",
    situation: "주간 회의 정리",
    title: "주간 회의록",
    desc: "회의 안건과 결정 사항, 후속 과제를 정리해 공유하는 회의록 템플릿입니다.",
    preview: [
      "1. 회의 개요",
      "2. 주요 안건",
      "3. 결정 사항",
      "4. 후속 조치",
    ],
  },
  {
    id: "tpl-minutes-kickoff",
    type: "MINUTES",
    category: "회의록",
    dept: "디자인팀",
    situation: "프로젝트 킥오프",
    title: "프로젝트 킥오프 회의록",
    desc: "프로젝트 시작 회의에서 논의한 목표, 역할, 일정, 주요 의사결정을 정리합니다.",
    preview: [
      "1. 회의 목적",
      "2. 참여자 및 역할",
      "3. 주요 논의 내용",
      "4. 액션 아이템",
    ],
  },
  {
    id: "tpl-approval-education",
    type: "APPROVAL",
    category: "결재",
    dept: "인사팀",
    situation: "외부 교육 참가 요청",
    title: "교육 참가 결재 요청",
    desc: "교육 참가 필요성과 기대 효과를 정리하는 결재 사유 템플릿입니다.",
    preview: [
      "1. 결재 개요",
      "2. 교육 정보",
      "3. 요청 사유",
      "4. 기대 효과",
    ],
  },
  {
    id: "tpl-approval-purchase",
    type: "APPROVAL",
    category: "결재",
    dept: "경영지원팀",
    situation: "구매 요청",
    title: "구매 결재 요청",
    desc: "업무 수행에 필요한 구매 사유와 기대 효과를 정리하는 템플릿입니다.",
    preview: [
      "1. 구매 요청 개요",
      "2. 구매 대상",
      "3. 요청 사유",
      "4. 예상 비용",
    ],
  },
  {
    id: "tpl-report-design-review",
    type: "REPORT",
    category: "보고",
    dept: "디자인팀",
    situation: "디자인 검토 결과 공유",
    title: "디자인 검토 보고서",
    desc: "디자인 시안 검토 결과와 수정 의견을 정리해 공유하는 보고서 템플릿입니다.",
    preview: [
      "1. 검토 개요",
      "2. 주요 확인 사항",
      "3. 수정 의견",
      "4. 후속 작업",
    ],
  },
  {
    id: "tpl-minutes-dev-sync",
    type: "MINUTES",
    category: "회의록",
    dept: "개발1팀(BE), 개발2팀(FE)",
    situation: "협업 회의 정리",
    title: "개발 협업 회의록",
    desc: "개발팀 간 협업 회의에서 나온 결정 사항과 작업 분담을 정리합니다.",
    preview: [
      "1. 회의 목적",
      "2. 논의 내용",
      "3. 결정 사항",
      "4. 담당 작업",
    ],
  },
];
