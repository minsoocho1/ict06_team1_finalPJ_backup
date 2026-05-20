# ict06_team1_finalPJ

ICT06 의 1팀 최종 프로젝트 깃허브 저장소.

## 프로젝트 주제

AI 융합 리액트 기반 사내 그룹웨어 & 교육평가시스템 플랫폼

## 프로젝트 실행 환경 설정

### 1. 프로젝트 경로

1. `D:\DEV06\` 에서 git bash 실행 -> git clone하여 프로젝트 설치 (D 드라이브 없는 경우 `C:\DEV06\`에서 클론)

    ```bash
    git clone https://github.com/youngeunsong/ict06_team1_finalPJ.git
    ```

1. `ict06_team1_finalPJ` 폴더 안에서 git bash 실행
1. 다음 코맨드를 git bash에서 차례대로 실행하여 최신 원격 dev 브랜치와 동일한 상태로 갱신

    ```bash
    git checkout -b dev
    git pull origin dev 
    ```

### 2. [백엔드] 인텔리제이 환경설정 & 실행

1. **프로젝트 폴더 열기**: `Open` -> `D:\DEV06\ict06_team1_finalPJ` 선택  

1. **인코딩 설정** : Settings에서 `Encod` 검색. 아래 이미지처럼 모두 UTF-8로 통일
![인코딩](/readme_images/intellij-encoding.png)

1. **깃허브로 공유하면 안 되는 파일 다운로드** : `구글 공유 드라이브(ICT06_최종프로젝트)/환경설정_백업` 경로에 깃허브로 공유해서는 안 되는 파일을 백업해놨으니, 여기에서 별도로 다운로드 받으셔서 프로젝트 내에 위치시켜 주세요.

    * 각 폴더 별 파일은 아래의 경로에 위치시킵니다:
        * properties 폴더 내용물: `src/main/resources/` 폴더 안 (원격 DB 연결용)
        * ai_server 폴더 내용물: `ai_server` 폴더 안 (파이썬에서 사용하는 api 키)

    **⚠️ 개인정보, 보안 등의 이슈와 관련된 파일은 절대 github에 공유하지 않아야 합니다.**
    이런 파일들은 반드시 구글 드라이브를 이용해 공유해주시고, .gitignore에 파일명을 등록하여 깃허브에 올라가지 않게 방지해주세요.

    ```plain
    예시: DB 계정명과 비밀번호, API 서비스키 
    ```  

1. **로컬 DB 사용 시 주의사항**
    * 로컬DB 연결하기: 프로젝트 처음 받으면 기본적으로 서버컴에 연결된 상태입니다. 혹시 로컬 DB를 이용해 테스트하실려면 `application-local.properties`의 내용 일부를 다음과 같이 수정해주세요:

        ```bash
        spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
        ```

    * 계정생성 기능 없는 상태로 해싱 적용한 로그인 기능 먼저 구현함. 따라서 DB에는 해싱값이 들어있고, 브라우저에서 사용자가 입력하는 값은 평문이어야 함
    * 이를 위해 프로젝트 실행 시 가장 먼저 `TempPassword.java` 파일 실행하여 해싱값을 생성 -> IntelliJ 콘솔에서 DB password 값 확인하여 test 계정 데이터를 update 처리하면 해싱된 값으로 저장됨.

1. **백엔드용 port 번호 변경 필요시**
    * 백엔드: `application.properties` 에서 포트 번호 변경
    * 프론트엔드: `LoginPage.js`, `Crawling.js`에서 포트 번호 변경
1. `src/main/java/com/ict06/team1_fin_pj/Team1FinPjApplication.java`를 실행. 정상 실행되는 지 확인.  

### 3. [프론트엔드] VisualStudio Code 환경설정 & 실행

1. **프로젝트 폴더 열기**:`File` -> `Open Folder` -> `D:\DEV06\ict06_team1_finalPJ` 선택

1. **프론트엔드 작업용 폴더로 이동하기**: `Terminal` -> `New Terminal` -> `+` 옆의 `V` -> `command prompt` 선택-> cmd에서 `cd react-frontend` 입력

1. **처음 깃허브에서 clone해오거나 pull 해온 직후 입력**: **반드시** `npm install`**을 먼저 cmd에 입력**해주세요. 다른 팀원이 추가한 라이브러리를 설치하는 과정입니다.

1. **Q클래스 생성** : 
    VSCode
    좌측 하단 MAVEN 탭
    com.ict06:team1_fin_pj > Lifecycle > clean(실행), compile(실행)
    *clean: 터미널에 BUILD SUCCESS 뜨면 성공
    *compile: Q클래스 만드는 작업

1. **프로젝트 실행**: `npm start`를 cmd에 입력.
    1. `Module not found` 에러 발생 시 : 해당 모듈을 cmd에서 설치하면 오류가 사라집니다.

        ```bash
        # 예시) Module not found : react-router-dom 에러 발생 시  
        npm i react-router-dom
        ```

### 4. [AI서버] VisualStudio Code 환경설정 & 실행

1. **프로젝트 폴더 열기**:`File` -> `Open Folder` -> `D:\DEV06\ict06_team1_finalPJ` 선택

2. **AI 서버 작업용 폴더로 이동하기**: `Terminal` -> `New Terminal` -> `+` 옆의 `V` -> `Git bash` 선택-> `cd ai_server` 입력

3. **가상환경 생성하기**: (2번의 ai_server 폴더로 이동한 상태에서) `python -m venv venv` 입력    

4. **가상환경 활성화 및 라이브러리 설치**: `source venv/Scripts/activate` 입력
   **정상적으로 실행되면 터미널의 경로 앞에 `(venv)` 라는 표시가 생깁니다.**

5. **라이브러리 설치**: `pip install -r requirements.txt` 입력

6. **AI 가상 서버 띄우기**: (Terminal에서) `uvicorn main:app --reload` 입력 -> 실행 성공 시 터미널에 `INFO: Application startup complete.` 메시지가 뜹니다.

7. **정상 실행되지 않을 시**: (Terminal의 ai_server 폴더에서) `pip install -r requirements.txt` 입력하여 DB 접속 라이브러리를 설치한 후 3,4번을 다시 실행합니다.

8. **AI 서버 실행 확인**: http://127.0.0.1:8000 로 접속 -> 화면에 응답이 있으면 성공입니다.

### 5. [AI서버-Ollama] 다운로드 및 설치 & 실행
1. **파일 다운로드 및 설치**: https://ollama.com/download 에서 윈도우즈용 다운로드 후 실행

2. **Ollama 모델 받기**: (VSCode의 Terminal-Powershell에서)
   `cd ai_server > Get-ChildItem "C:\Users\ictedu\AppData\Local\Programs\Ollama" -File` -> 결과에 ollama.exe 보이면 다음 명령어 입력
    - Ollama 모델 설치: `& "C:\Users\ictedu\AppData\Local\Programs\Ollama\ollama.exe" pull llama3.1:8b`
    - 모델 목록 확인: `& "C:\Users\ictedu\AppData\Local\Programs\Ollama\ollama.exe" list`

3. **실행 테스트**: `& "C:\Users\ictedu\AppData\Local\Programs\Ollama\ollama.exe" run llama3.1:8b "아무 질문이나 입력하기”` -> 입력한 질문에 대한 AI 답변이 나오면 성공

4. **Ollama 설정 켜기**: .env 파일에서 `OLLAMA_DOC_POSTPROCESS=true`로 수정(기본값 false이면 Ollama 실행하지 않음)

5. **AI서버 재시작하기**: (Terminal-git bash에서) 4-6(AI 가상 서버 띄우기)번의 명령어 실행

### 6. [Redis] 다운로드 및 설치 & 실행
1. **파일 다운로드 및 설치**: https://github.com/tporadowski/redis/releases 에서 msi파일 다운로드 후 실행
   **설치 오류 시**: 위 경로에서 zip파일 다운로드 -> 프로젝트 폴더(...\ict06_team1_finalPJ)에 redis 폴더 생성 후 zip파일 넣어 압축 풀기 -> redis-server.exe 실행

2. **Ollama 서버 연결**: (VSCode > Terminal에서) `.\redis\redis-cli.exe ping` 입력 -> 결과 `pong` 나오면 Redis 서버 연결 성공

3. **확장 프로그램 설치**: (VSCode > Extension에서) `Redis` 설치

4. **서버 생성 확인**: 좌측 사이드바의 Service 아이콘 -> Create Connection -> Connect to server -> 설정값 입력 후 Connect
   -> 왼편 Service 영역에 Redis 서버 생성되었는지 확인
   **설정값**: Host: 127.0.0.1 / Port: 6379

### 로그인 테스트
1. **테스트 계정으로 로그인**: 이제 프로젝트를 실행하면 테스트 계정의 사번/비밀번호로 로그인이 가능
   * **테스트 계정**: 사번 20209999 / 비밀번호: 1234
    ![alt text](/readme_images/login.png)

### ⚠️실행 시 주의점

v.260420

* 계정생성 기능 없는 상태로 해싱 적용한 로그인 기능 먼저 구현함. 따라서 DB에는 해싱값이 들어있고, 브라우저에서 사용자가 입력하는 값은 평문이어야 함
* 이를 위해 프로젝트 실행 시 가장 먼저 'TempPassword.java' 파일 실행하여 해싱값을 생성 -> IntelliJ 콘솔에서 DB password 값 확인하여 test 계정 데이터를 update 처리하면 해싱된 값으로 저장됨.
* 이제 프로젝트를 실행하면 사번/비밀번호로 로그인이 가능
  * **테스트 계정**: 사번 20209999 / 비밀번호: 1234

## 프로젝트 아키텍쳐

### 전체 구조 개괄

* ⚠️‘coreUI 템플릿 제공’ 이란 설명이 붙은 폴더/파일은 저희가 리액트용으로 사용하는 템플릿에서 제공하는 파일입니다. 파일 내용이나 위치를 옮기지 않으셔야 합니다.
* 주로 사용하거나 편집할 것으로 예상되는 파일명/폴더명 앞에 ⭐을 붙여놨으니 참고.
* 알파벳 순으로 정렬 (비주얼스튜디오, 인텔리제이에서 정렬되어 보이는 것과 가급적 같게 통일)

<details>
<summary>전체 구조 개괄 접기/펼치기</summary>

```xml
ict06_team1_finalPJ                         # 스프링부트 메인 프로젝트
├── ai_server/                              # ⭐AI 작업용 파일
│   └── .env                                # ⚠️파이썬에 필요한 API 키 등 저장. github에 안 올라감
├── DB/                                     # ⭐DB 작업 시 필요한 쿼리/데이터 보관
├── react-frontend/                         # 일반 직원/팀장용 React 프론트엔드
│   ├── .gitignore                          # React 용 .gitignore 설정
│   ├── src/
│   │   ├── api/                            # ⭐백엔드 API 호출 함수 모음
│   │   ├── assets/
│   │   │   ├── brand                       # coreUI 템플릿 제공
│   │   │   └── images/                     # ⭐프로젝트 구현에 필요한 이미지
│   │   │       ├── aiSecretary/                # 대분류: AI 비서 / 초안 작성·문서 교정·요약
│   │   │       ├── attendance/                 # 대분류: 근태관리 / 출근·퇴근·조퇴·휴가·외근·초과근무
│   │   │       ├── auth/                       # 대분류: 인증/인가 / 로그인·마이페이지
│   │   │       ├── avatars/                    # coreUI 템플릿 제공 - 프로필 이미지
│   │   │       ├── calendar/                   # 대분류: 캘린더 / 일정 등록·조회·수정·삭제
│   │   │       ├── chatbot/                    # 대분류: 챗봇 / 사내 규정 Q&A·서비스 안내
│   │   │       ├── common/                     # 여러 페이지에서 공통적으로 필요한 이미지 
│   │   │       ├── employee/                   # 대분류: 인사 관리 / 내 정보·조직도 조회
│   │   │       ├── evaluation/                 # 대분류: 인사평가(교육평가 AI 포함) / AI 퀴즈·자기평가·결과 조회
│   │   │       ├── first_demo/                 # 1차 시연 시 필요한 화면 스크린샷
│   │   │       │   └── rename.js               # 사진 파일명 일괄변경 파일. 한글명과 영어명 대응 시 참고.   
│   │   │       ├── onboarding/                 # 대분류: 인사평가(교육평가 AI 포함) / 온보딩 로드맵·체크리스트·교육일정
│   │   │       └── payroll/                    # 대분류: 급여 관리 / 개인 급여 조회·명세서 확인
│   │   │
│   │   ├── components/                    # 공통 컴포넌트 (CoreUI 제공 컴포넌트 포함)
│   │   │   ├── header/                    # CoreUI 제공
│   │   │   ├── RealtimeAlert.js           # ⭐실시간 알림 화면
│   │   │   └── PageTemplate.js            # ⭐프론트엔드 페이지 템플릿 (이걸 활용하여 페이지 만들어주세요)
│   │   │
│   │   ├── constants/                     # 공통 상수
│   │   │   └── path.js                    # ⭐경로 상수 (이 파일을 이용해 화면 이동 구현해주세요)
│   │   │
│   │   ├── helpers/                     
│   │   │   └── axios_helper.js            # ⭐Axios를 이용하여 REST API를 지원. 백엔드 통신용 공통 메써드 request 포함. 
│   │   │
│   │   ├── layout/                        # coreUI 템플릿 제공
│   │   ├── pages/                         # ⭐사용자 화면 페이지
│   │   │   ├── aiSecretary/               # 대분류: AI 비서 / 초안 작성·문서 교정·요약
│   │   │   ├── attendance/                # 대분류: 근태관리 / 출근·퇴근·조퇴·휴가·외근·초과근무
│   │   │   ├── approval/                  # 대분류: 전자결재
│   │   │   ├── auth/                      # 대분류: 인증/인가 / 로그인·마이페이지
│   │   │   ├── calendar/                  # 대분류: 캘린더 / 일정 등록·조회·수정·삭제
│   │   │   ├── chatbot/                   # 대분류: 챗봇 / 사내 규정 Q&A·서비스 안내
│   │   │   ├── employee/                  # 대분류: 인사 관리 / 내 정보·조직도 조회
│   │   │   ├── evaluation/                # 대분류: 인사평가(교육평가 AI 포함) / AI 퀴즈·자기평가·결과 조회
│   │   │   ├── onboarding/                # 대분류: 인사평가(교육평가 AI 포함) / 온보딩 로드맵·체크리스트·교육일정
│   │   │   ├── payroll/                   # 대분류: 급여 관리 / 개인 급여 조회·명세서 확인
│   │   │   └── test/                      # 📚 Test 예제
│   │   │
│   │   ├── routes/                        # ⭐도메인 별 경로 관리 폴더 (path.js 의 경로와 화면 구현 js 파일 연결)
│   │   │   ├── aiPortalRoutes.js                  # 사내 AI 포탈
│   │   │   ├── aiSecretaryRoutes.js               # 대분류: AI 비서 / 초안 작성·문서 교정·요약
│   │   │   ├── alertRoutes.js                     # 대분류: 알림
│   │   │   ├── approvalRoutes.js                  # 대분류: 전자결재
│   │   │   ├── attendanceRoutes.js                # 대분류: 근태관리 / 출근·퇴근·조퇴·휴가·외근·초과근무
│   │   │   ├── authRoutes.js                      # 대분류: 인증/인가 / 로그인·마이페이지
│   │   │   ├── calendarRoutes.js                  # 대분류: 캘린더 / 일정 등록·조회·수정·삭제
│   │   │   ├── chatbotRoutes.js                   # 대분류: 챗봇 / 사내 규정 Q&A·서비스 안내
│   │   │   ├── employeeRoutes.js                  # 대분류: 인사 관리 / 내 정보·조직도 조회
│   │   │   ├── evaluationRoutes.js                # 대분류: 인사평가(교육평가 AI 포함) / AI 퀴즈·자기평가·결과 조회
│   │   │   ├── index.js                           # ⭐모든 도메인별 경로 파일 포함. 
│   │   │   ├── onboardingRoutes.js                # 대분류: 인사평가(교육평가 AI 포함) / 온보딩 로드맵·체크리스트·교육일정
│   │   │   ├── payrollRoutes.js                   # 대분류: 급여 관리 / 개인 급여 조회·명세서 확인
│   │   │   └── testRoutes.js                      # 📚 Test 예제
│   │   │
│   │   ├── scss/                          # coreUI 템플릿 제공
│   │   │   └── vendors/                   # coreUI 템플릿 제공
│   │   │
│   │   ├── store/                         # React 전역 상태관리(로그인 정보, 권한, 알림 등)
│   │   ├── styles/                        # css 관련 파일
│   │   │   ├── css                        # css 형식 스타일
│   │   │   └── js                         # js 형식 스타일 
│   │   │       └── demoPageStyle.js       # 1차 시연용 CSS를 구현한 js
│   │   └── views/                         # coreUI 템플릿 제공 페이지: 우리의 pages 폴더와 유사한 역할
│   ├── package-lock.json
│   └── package.json
│ 
├── src/main/                              # ⭐백엔드
│   ├── java/com/ict06/team1_fin_pj/
│   │   ├── common/                        # ⭐공통 영역
│   │   │   ├── config/                    # 스프링 설정, 보안 설정, 웹 설정
│   │   │   ├── dto/                       # DTO
│   │   │   │   ├── aiSecretary/               # 대분류: AI 비서 / 초안 작성·문서 교정·요약
│   │   │   │   ├── attendance/                # 대분류: 근태관리 / 출근·퇴근·조퇴·휴가·외근·초과근무
│   │   │   │   ├── approval/                  # 대분류: 전자결재
│   │   │   │   ├── auth/                      # 대분류: 인증/인가 / 로그인·마이페이지
│   │   │   │   ├── calendar/                  # 대분류: 캘린더 / 일정 등록·조회·수정·삭제
│   │   │   │   ├── chatbot/                   # 대분류: 챗봇 / 사내 규정 Q&A·서비스 안내
│   │   │   │   ├── common/                    # 도메인에 해당하지 않는 공통적으로 사용하는 dto 배치 
│   │   │   │   ├── employee/                  # 대분류: 인사 관리 / 내 정보·조직도 조회
│   │   │   │   ├── evaluation/                # 대분류: 인사평가(교육평가 AI 포함) / AI 퀴즈·자기평가·결과 조회
│   │   │   │   ├── onboarding/                # 대분류: 인사평가(교육평가 AI 포함) / 온보딩 로드맵·체크리스트·교육일정
│   │   │   │   └── payroll/                   # 대분류: 급여 관리 / 개인 급여 조회·명세서 확인
│   │   │   ├── exception/                 # 예외 처리
│   │   │   ├── response/                  # 공통 응답 형식
│   │   │   ├── util/                      # 유틸 클래스
│   │   │   └── security/                  # JWT, 권한 체크 공통 처리
│   │   │
│   │   ├── domain/                        # ⭐기능(도메인) 중심 모듈
│   │   │   ├── attendance/                # 대분류: 근태관리
│   │   │   │   └── ※ 내부에 MVC 구조 포함
│   │   │   │      - 기본근무: 출근 등록, 퇴근 등록, 조퇴 관리, 근태 대시보드
│   │   │   │      - 휴가관리: 연차 현황, 휴가계 조회, 연차 생성
│   │   │   │      - 외근/출장: 외근 등록
│   │   │   │      - 초과근무: 연장 신청
│   │   │   │      - 관리자 기능: 근태 유형 설정, 전사/부서 근태 현황·통계, 출퇴근 기록 수정, 엑셀 출력
│   │   │   │
│   │   │   ├── approval/                  # 대분류: 전자결재
│   │   │   │   └── ※ 내부에 MVC 구조 포함
│   │   │   │      - 새 결재 진행: 결재 정보 선택, 결재 내용 작성, OCR(후순위)
│   │   │   │      - 개인 문서함 / 임시 저장함
│   │   │   │      - 팀장 기능: 결재 대기 문서함, 결재 예정 문서함, 승인/반려 처리
│   │   │   │      - 관리자 기능: 결재 서식 관리, 결재선 관리
│   │   │   │
│   │   │   ├── calendar/                  # 대분류: 캘린더
│   │   │   │   └── ※ 내부에 MVC 구조 포함
│   │   │   │      - 일정 등록, 조회, 상세, 수정, 삭제, 검색
│   │   │   │      - 일정 구분(개인/부서/공통), 카테고리 표시
│   │   │   │      - 일정 이동(드래그 앤 드롭)
│   │   │   │      - 관리자 기능: 일정 권한 처리, 공휴일 관리
│   │   │   │
│   │   │   ├── employee/                  # 대분류: 인사 관리
│   │   │   │   └── ※ 내부에 MVC 구조 포함
│   │   │   │      - 사원관리: 사원 등록, 목록 조회, 상세 조회, 수정, 삭제
│   │   │   │      - 권한관리: 접근 권한 처리
│   │   │   │      - 조직관리: 조직도 전체 조회, 부서별 조회, 구성원 상세
│   │   │   │      - 관리자 기능: 부서 관리, 직급 관리
│   │   │   │
│   │   │   ├── payroll/                   # 대분류: 급여 관리
│   │   │   │   └── ※ 내부에 MVC 구조 포함
│   │   │   │      - 급여 대장 관리: 대장 생성, 저장, 마감, 삭제
│   │   │   │      - 기본급 관리: 등록, 수정, 삭제, 목록 조회
│   │   │   │      - 급여 요약: 급여 목록, 검색 필터, 상세 보기, 변동 추이
│   │   │   │      - 급여 성과 분석: 교육 성취도-보상 분석
│   │   │   │      - 사용자 기능: 개인 급여 조회, 증빙서류 셀프 발급
│   │   │   │
│   │   │   ├── onboarding/                # 대분류: 인사평가(교육평가 AI 포함) - 온보딩 영역
│   │   │   │   └── ※ 내부에 MVC 구조 포함
│   │   │   │      - 온보딩: 콘텐츠 관리, 교육 로드맵 설계
│   │   │   │      - 사용자 기능: 직무별 로드맵 조회, 진행률 표시, 체크리스트 확인
│   │   │   │      - 교육 일정 생성 및 관리, 일정 알림
│   │   │   │
│   │   │   ├── evaluation/                # 대분류: 인사평가(교육평가 AI 포함) - 평가 영역
│   │   │   │   └── ※ 내부에 MVC 구조 포함
│   │   │   │      - AI 온보딩 평가: 퀴즈, 답변 저장, 유사도 점수 계산
│   │   │   │      - 이해도 분석: 자기 평가, AI 평가 결과 조회, 비교 분석 및 피드백
│   │   │   │      - AI 학습 보강: 컨텍스트 기반 답변 생성, 콘텐츠 요약 및 재설명
│   │   │   │      - 평가 조회: 대시보드, 개인 평가 결과, 진행할 평가 목록
│   │   │   │      - 관리자 기능: 평가 현황, 평가 점수 통계, 평가 항목 설정, 이탈 징후 분석
│   │   │   │
│   │   │   ├── aiSecretary/               # 대분류: AI 비서
│   │   │   │   └── ※ 내부에 MVC 구조 포함
│   │   │   │      - 문서 작성: 보고서 초안, 결재 사유 생성, 문서 교정, 템플릿 생성
│   │   │   │      - 요약 + 액션: 회의록 요약, 데일리 업무 추천
│   │   │   │      - 관리자 기능: 참조 문서 권한 검증, 생성 결과 권한 제한, 비인가 참조 차단
│   │   │   │
│   │   │   ├── chatbot/                   # 대분류: 챗봇
│   │   │   │   └── ※ 내부에 MVC 구조 포함
│   │   │   │      - 지식 베이스: 서비스 이용 안내, 사내 규정 Q&A(RAG), 유사 질문 추천
│   │   │   │      - 관리자 기능: 권한 기반 문서 검색, 출처 노출 제어, 비인가 문서 응답 차단, 링크 접근 제어
│   │   │   │
│   │   │   ├── auth/                      # 대분류: 인증/인가
│   │   │   │   └── ※ 내부에 MVC 구조 포함
│   │   │   │      - 계정: 로그인
│   │   │   │      - 마이페이지: 내 정보 조회/수정
│   │   │   │      - 관리자 기능: 권한 분리 및 접근 제어
│   │   │   │
│   │   │   ├── notification/              # 대분류: 알림
│   │   │   │   └── ※ 내부에 MVC 구조 포함
│   │   │   │      - 실시간 알림 처리 (출근 미체크, 퇴근 권장 등)
│   │   │   │
│   │   │   └── aiOps/                     # 대분류: AI 데이터 운영
│   │   │       └── ※ 내부에 MVC 구조 포함
│   │   │          - AI 데이터 수집: 수집 현황 대시보드, 통합 로그 조회, CSV/엑셀 다운로드
│   │   │          - AI 활용 로그: 챗봇 로그, AI 비서 사용 로그, 사용량 통계
│   │   │          - 데이터 관리: 벡터 데이터 관리, 문서 권한 메타데이터 저장
│   │   │          - 분석·추천: 질문/문서 로그 분석, FAQ 자동화
│   │   │          - RAG 검색 제어: 사용자 권한 기반 필터링
│   │   │
│   │   ├── external/                      # 외부 API/AI 연동 전용
│   │   │   ├── ai/                        # Ollama, LangChain, 임베딩, 벡터DB
│   │   │   ├── ocr/                       # CLOVA OCR
│   │   │   ├── maps/                      # Kakao/Google Maps
│   │   │   ├── weather/                   # OpenWeather 등
│   │   │   ├── mail/                      # Gmail SMTP 등
│   │   │   ├── crawling/                  # 뉴스/외부 데이터 크롤링
│   │   │   └── storage/                   # 파일 업로드, 이미지/PDF 처리
│   │   └── test                           # 📚 Test 예제

│   │
│   └── resources/                         # 리소스
│       ├── mappers/                       # 매퍼
│       │   ├── attendance/                # 대분류: 근태관리 / 출근·퇴근·조퇴·휴가·외근·초과근무
│       │   ├── approval/                  # 대분류: 전자결재 / 상신·임시저장·문서함·결재대기/예정
│       │   ├── calendar/                  # 대분류: 캘린더 / 일정 등록·조회·수정·삭제
│       │   ├── employee/                  # 대분류: 인사 관리 / 내 정보·조직도 조회
│       │   ├── payroll/                   # 대분류: 급여 관리 / 개인 급여 조회·명세서 확인
│       │   ├── onboarding/                # 대분류: 인사평가(교육평가 AI 포함) / 온보딩 로드맵·체크리스트·교육일정
│       │   ├── evaluation/                # 대분류: 인사평가(교육평가 AI 포함) / AI 퀴즈·자기평가·결과 조회
│       │   ├── aiSecretary/               # 대분류: AI 비서 / 초안 작성·문서 교정·요약
│       │   ├── chatbot/                   # 대분류: 챗봇 / 사내 규정 Q&A·서비스 안내
│       │   └── auth/                      # 대분류: 인증/인가 / 로그인·마이페이지
│       │
│       ├── static/                        # 관리자 타임리프용 css/js/image
│       │   ├── css/
│       │   │   ├── adminlte.css
│       │   │   └── adminlte.min.css
│       │   ├── js/
│       │   │   ├── adminlte.js
│       │   │   └── adminlte.min.js
│       │   └── images/                    # ⭐타임리프 페이지에서 필요한 이미지 파일들  
│       │
│       ├── templates/                     # Thymeleaf HTML
│       │   ├── index.html                 # AdminLTE 템플릿 대표 예시 파일. 참고용. 
│       │   └── admin/                     # ⭐관리자용 화면
│       │       ├── attendance/            # 대분류: 근태관리 / 전사 근태 현황·통계·근태유형 설정
│       │       ├── approval/              # 대분류: 전자결재 / 결재 서식·결재선 관리
│       │       ├── calendar/              # 대분류: 캘린더 / 관리자 일정·권한 관리
│       │       ├── employee/              # 대분류: 인사 관리 / 사원·부서·직급 관리
│       │       ├── payroll/               # 대분류: 급여 관리 / 급여대장·기본급·수당·공제
│       │       ├── onboarding/            # 대분류: 인사평가(교육평가 AI 포함) / 온보딩 콘텐츠·로드맵 관리
│       │       ├── evaluation/            # 대분류: 인사평가(교육평가 AI 포함) / 평가 현황·통계·이탈 징후 분석
│       │       ├── aiOps/                 # 대분류: AI 데이터 운영 / AI 로그·벡터 데이터·운영 대시보드
│       │       ├── auth/                  # 대분류: 인증/인가 / 관리자 로그인·권한 화면
│       │       ├── test/                  # 📚 Test 예제
│       │       └── common/                # 공통 레이아웃, 헤더, 사이드바
│       │           ├── commonJs.html      # 공통 사용 js 모음
│       │           ├── footer.html        # 푸터
│       │           ├── head.html          # head 공통 요소: 폰트, adminlte css, fontawesome 등 포함
│       │           ├── header.html        # 헤더
│       │           ├── sidebar.html       # 사이드바
│       │           └── template.html      # ⭐ 관리자용 화면 템플릿 (이 파일을 이용하여 다른 파일을 만들어주세요)
│       │
│       ├── application.properties         # ⚠️DB 계정 등 환경설정 (깃허브 업로드 X)
│       ├── application-local.properties   # ⚠️DB 계정 등 환경설정 (깃허브 업로드 X)
│       └── application-prod.properties    # ⚠️DB 계정 등 환경설정 (깃허브 업로드 X)
│
├── .gitignore                             # ⭐Springboot 용 gitignore 설정
├── readme_images/                         # ⭐readme.md 작업 시 필요한 이미지 파일 
└── pom.xml                                # ⚠️백엔드에 필요한 디펜던시 관리
```

</details>

### (백엔드) domain의 각 하위폴더 & test 폴더 패키지 내 구조

domain의 각 하위폴더 & test 폴더의 각 패키지 내부는 MVC 패턴으로 구현. 
<details>
<summary>전체 구조 개괄 접기/펼치기</summary>

```plain
예시)
Attendance/ 
├── controller/
│   ├── AttendanceController   # 팀원용
│   └── adAttendanceController # 관리자용
│
├── entity/                     # DB의 테이블과 1:1 대응되는 엔티티 
│
├── service/
│   ├── AttendanceService       # 팀원용 인터페이스
│   ├── AttendanceServiceImpl   # 팀원용 클래스
│   ├── adAttendanceService     # 관리자용 인터페이스
│   └── adAttendanceServiceImpl # 관리자용 클래스
│
└── repository/
    ├── AttendanceRepo          # JPA 연결 리포지토리 (인터페이스). JPA 방식 사용 시 ServiceImpl에서 이걸 로드. 
    ├── AttendanceRepoCustom    # QueryDSL과 연결하는 리포지토리 (인터페이스)
│   └── AttendanceRepoImpl      # 리포지토리 구현 클래스. QueryDSL 사용 시 ServiceImpl에서 이걸 로드. 

```

</details>

⚠️ mappers는 resources 폴더 하위에 위치, dto는 common폴더에 위치시킴. 
<details>
<summary>mapper, dto 구조</summary>

```plain
# mappers 예시)
resources
└── mappers
    ├── AttendanceMapper   # 팀원용
    └── adAttendanceMapper # 관리자용

# dto 위치)    
│   ├── java/com/ict06/team1_fin_pj/
│   │   ├── common/                        # ⭐공통 영역
│   │   │   ├── dto/                       # DTO
│   │   │   │   ├── aiSecretary/               # 대분류: AI 비서 / 초안 작성·문서 교정·요약
│   │   │   │   ├── attendance/                # 대분류: 근태관리 / 출근·퇴근·조퇴·휴가·외근·초과근무
│   │   │   │   ├── approval/                  # 대분류: 전자결재
│   │   │   │   ├── auth/                      # 대분류: 인증/인가 / 로그인·마이페이지
│   │   │   │   ├── calendar/                  # 대분류: 캘린더 / 일정 등록·조회·수정·삭제
│   │   │   │   ├── chatbot/                   # 대분류: 챗봇 / 사내 규정 Q&A·서비스 안내
│   │   │   │   ├── common/                    # 도메인에 해당하지 않는 공통적으로 사용하는 dto 배치 
│   │   │   │   ├── employee/                  # 대분류: 인사 관리 / 내 정보·조직도 조회
│   │   │   │   ├── evaluation/                # 대분류: 인사평가(교육평가 AI 포함) / AI 퀴즈·자기평가·결과 조회
│   │   │   │   ├── onboarding/                # 대분류: 인사평가(교육평가 AI 포함) / 온보딩 로드맵·체크리스트·교육일정
│   │   │   │   └── payroll/                   # 대분류: 급여 관리 / 개인 급여 조회·명세서 확인   

```
</details>

## 📌 라우팅 경로 관리 구조 정리 (path / routes 분리)

라우팅 경로를 중앙에서 관리할 수 있는 구조

---

### 1. path.js (경로 상수 관리)

* 모든 URL 경로를 `path.js`에서 상수로 관리하도록 변경

```jsx
// path.js
export const PATH = {
  AI_SECRETARY: {
    ANSWER_TO_CHAT: '/ai-portal/secretary/answer-to-chat',
  },
  ATTENDANCE: {
    MAIN: '/attendance',
  },
}
```

👉 문자열 하드코딩 제거
👉 경로 변경 시 한 곳만 수정하면 됨

---

### 2. routes/index.js (라우팅 정의 분리)

* 실제 라우팅 설정을 `routes/도메인명Routes.js`로 분리

```jsx
import { PATH } from './path'
import AnswerToChat from '../pages/aiSecretary/AnswerToChat'

const AISecretaryRoutes = [
  {
    path: PATH.AI_SECRETARY.ANSWER_TO_CHAT,
    element: <AnswerToChat />,
  },
]

export default routes
```

👉 라우팅 구조를 한 눈에 파악 가능
👉 페이지 추가 시 이 파일만 수정하면 됨

---

### 3. App.js에서 routes 사용

```jsx
import { getAppRoutes } from "./routes/index";

...

{/* 3. [그룹 B] 사이드바/헤더 있는 메인 서비스 레이아웃 */}
{/* 메인 : routes/index.js에 정리된 모든 경로 지원*/}
{/* 앞으로 생성할 페이지는 routes/의 각 대분류 별 파일에 Route만 추가하면 사이드바가 자동으로 적용됨 */}
<Route element={<DefaultLayout />}>
    {routes.map((route, idx) => (
    <Route key={idx} path={route.path} element={route.element} />
    ))}
</Route>
```

👉 라우팅 로직 단순화

---

### 4. 실제 사용 방식 (Link)

```jsx
import { PATH } from '@/routes/path'

<Link to={PATH.AI_SECRETARY.ANSWER_TO_CHAT}>
  <CButton>이동</CButton>
</Link>
```

👉 경로를 직접 쓰지 않고 PATH 사용

---

### 📁 구조 요약

```plaintext
│   │   ├── constants/                     # 공통 상수
│   │   │   └── path.js                    # ⭐경로 상수 (이 파일을 이용해 화면 이동 구현해주세요)
│   │   ├── routes/                        # ⭐도메인 별 경로 관리 폴더 (path.js 의 경로와 화면 구현 js 파일 연결)
│   │   │   ├── aiPortalRoutes.js                  # 사내 AI 포탈
│   │   │   ├── aiSecretaryRoutes.js               # 대분류: AI 비서 / 초안 작성·문서 교정·요약
│   │   │   ├── alertRoutes.js                     # 대분류: 알림
│   │   │   ├── approvalRoutes.js                  # 대분류: 전자결재
│   │   │   ├── attendanceRoutes.js                # 대분류: 근태관리 / 출근·퇴근·조퇴·휴가·외근·초과근무
│   │   │   ├── authRoutes.js                      # 대분류: 인증/인가 / 로그인·마이페이지
│   │   │   ├── calendarRoutes.js                  # 대분류: 캘린더 / 일정 등록·조회·수정·삭제
│   │   │   ├── chatbotRoutes.js                   # 대분류: 챗봇 / 사내 규정 Q&A·서비스 안내
│   │   │   ├── employeeRoutes.js                  # 대분류: 인사 관리 / 내 정보·조직도 조회
│   │   │   ├── evaluationRoutes.js                # 대분류: 인사평가(교육평가 AI 포함) / AI 퀴즈·자기평가·결과 조회
│   │   │   ├── index.js                           # ⭐모든 도메인별 경로 파일 포함. 
│   │   │   ├── onboardingRoutes.js                # 대분류: 인사평가(교육평가 AI 포함) / 온보딩 로드맵·체크리스트·교육일정
│   │   │   └── payrollRoutes.js                   # 대분류: 급여 관리 / 개인 급여 조회·명세서 확인
```

---

### ✅ 기대 효과

* 경로 하드코딩 제거
* 경로 변경 시 영향 범위 최소화
* 라우팅 구조 한눈에 파악 가능
* 신규 페이지 추가 시 작업 위치 명확

---

### 🙏 사용 가이드 (중요)

1. **경로 추가 시**
   → `path.js`에 먼저 정의

2. **페이지 연결 시**
   → `routes/도메인명Routes.js`에 등록

3. **컴포넌트에서 이동 시**
   → `PATH` 상수 사용

---

### 📎 예시 흐름

```plaintext
path.js 추가 → routes/index.js 등록 → 페이지에서 PATH 사용
```

---

앞으로 라우팅 관련 작업은 위 구조 기준으로 진행하면 됩니다 👍

## 새로운 화면 생성 & 새 경로 설정 방법

### 일반 사용자용 (리액트 & 스프링부트)

1. 화면용 jsx 생성: 담당 도메인 폴더에 .js 형식 파일 새로 만들기
예시) `react-frontend\src\pages\approval` -> `ApprovalNew.js`

1. 화면 템플릿 이용하여 구현: `react-frontend\src\components\PageTemplate.js`와 `react-frontend\src\pages\test\TestList.js`에서 필요한 부분을 복사해오기.

1. 화면에 경로 맵핑하기: 총 2개의 파일에 작업 필요. 단순히 텍스트로 경로명을 쓸 땐 오류 발생 시 쉽게 찾기 어려워서 path.js 를 이용해 상수로 관리.  
    1. `react-frontend\src\constants\path.js` 에 추가
        예시)

        ```js
        // 대분류 : 전자결재
        APPROVAL: {
            ROOT: "/approval",                                      
            NEW: "/approval/new",   // ⭐ 새로 추가할 화면과 연결될 경로
            NEW_SELECT: "/approval/new/select-form",                
        ```

    1. `routes` 폴더에서 담당 대분류에 해당하는 경로 파일 찾아서 내용 추가.
        예시) '전자결재' 담당이라면 approvalRoutes.js 파일에 다음과 같은 형식으로 내용 추가

        ```js
        // 임포트 방법 1: lazy loading 적용할 것이면 
        const ApprovalNew = React.lazy(() => import( "../pages/approval/ApprovalNew"));

        // 임포트 방법 2: lazy loading 미적용 시
        import ApprovalNew from '../pages/approval/ApprovalNew';

        export const approvalRoutes = (userInfo) => [
            { path: PATH.APPROVAL.NEW, element: <ApprovalNew userInfo={userInfo} /> }, 
        ]
        ```

### 관리자용 (타임리프 & 스프링부트)

관리자 페이지는 리액트가 아닌 "타임리프"로 구현해야 됩니다.

* 템플릿을 이용한 타임리프 파일 작성 방법: `src/main/resources/templates/admin/common/template.html` 이 관리자 페이지를 만드는 템플릿입니다.
    1. `src/main/resources/templates`에서 담당 기능에 해당하는 폴더(예: `approval`)에 새 html 파일 만들기
    2. `template.html` 파일 내용을 복사 후 붙여넣기
    3. 다음과 같이 필요한 코드를 템플릿 사이에 작성하여 구현.

        ```html
        <!--begin::Container-->
        <div class="container-fluid">
        (담당 기능 구현 코드)
        </div>
        <!--end::Container-->
        ```

    4. MVC 패턴 구현: 각 담당 기능의 controller, service, repository 중 Ad로 시작하는 파일에서 관리자용 MVC 패턴 구현

* 관리자 페이지 접속 방법 (임시. 정식으로 로그인하는 방법 정해지면 아래 내용 수정하겠습니다)
    1. 다음 url로 접속 : `localhost:8081/admin/`
    2. 테스트 계정으로 로그인 (아이디: 20209999, 비밀번호: 1234)
    3. 로그인 후 `localhost:8081/admin/(관리자 페이지 url)`로 접속
    예) `localhost:8081/admin/template` => 템플릿 페이지로 이동
    (`com/ict06/team1_fin_pj/test/controller/TestController.java`에 url 맵핑된 상태)

* template.html 이 가진 공통 파일

```plain
template.html
├── head.html       # 공통 사용 css, font 등의 설정 포함
├── header.html
├── sidebar.html
├── footer.html
└── commonJs.html   # 공통 사용 js 파일 연결
```

⚠️주의사항

* 최신 AdminLTE를 적용하였습니다. 이미 최신 Bootstrap5과 Popper 등이 적용되어 있어서 새 html 파일에 또 해당 라이브러리를 불러오면 충돌이 일어납니다. 별도로 Bootstrap과 Popper를 넣지 않아야 정상 작동합니다.

## 프로젝트 적응용 예제 (Test 코드)

### Test 예제와 관련 깊은 파일 목록

```xml
ict06_team1_finalPJ                         # 스프링부트 메인 프로젝트
├── react-frontend/                         # 일반 직원/팀장용 React 프론트엔드
│   ├── src/
│   │   ├── constants/                     # 공통 상수
│   │   │   └── path.js                    # ⭐경로 상수 (이 파일을 이용해 화면 이동 구현해주세요)
│   │   │
│   │   ├── helpers/                     
│   │   │   └── axios_helper.js            # ⭐Axios를 이용하여 REST API를 지원. 백엔드 통신용 공통 메써드 request 포함. 
│   │   │
│   │   ├── pages/                         # ⭐사용자 화면 페이지
│   │   │   └── test/                      # 📚 Test 예제 (리액트)
│   │   │
│   │   ├── routes/                        # ⭐도메인 별 경로 관리 폴더 (path.js 의 경로와 화면 구현 js 파일 연결)
│   │   │   ├── index.js                           # ⭐모든 도메인별 경로 파일 포함. 
│   │   │   └── testRoutes.js                      # 📚 Test 예제 (리액트)
│   │   │
│   │   └── scss/                          # coreUI 템플릿 제공
│   │       └── vendors/                   # coreUI 템플릿 제공
│ 
├── src/main/                              # ⭐백엔드
│   ├── java/com/ict06/team1_fin_pj/
│   │   └── test                           # 📚 Test 예제 (리액트)
│   │       ├── controller/
│   │       │   ├── AttendanceController   # 팀원용
│   │       │   └── adAttendanceController # 관리자용
│   │       ├── entity/                     # DB의 테이블과 1:1 대응되는 엔티티 
│   │       ├── service/
│   │       │   ├── AttendanceService       # 팀원용 인터페이스
│   │       │   ├── AttendanceServiceImpl   # 팀원용 클래스
│   │       │   ├── adAttendanceService     # 관리자용 인터페이스
│   │       │   └── adAttendanceServiceImpl # 관리자용 클래스
│   │       └── repository/
│   │           ├── AttendanceRepo          # ⭐ JPA 연결 리포지토리 (인터페이스). JPA 방식 사용 시 ServiceImpl에서 이걸 로드. 
│   │           ├── AttendanceRepoCustom    # ⭐ QueryDSL과 연결하는 리포지토리 (인터페이스)
│   │           └── AttendanceRepoImpl      # ⭐ 리포지토리 구현 클래스. QueryDSL 사용 시 ServiceImpl에서 이걸 로드. 
│   │
│   └── resources/                         # 리소스 
│       ├── templates/                     # Thymeleaf HTML
│       │   └── admin/                     # ⭐관리자용 화면
│       │       ├── test/                  # 📚 Test 예제
│       │       └── common/                # 공통 레이아웃, 헤더, 사이드바
│       │           ├── commonJs.html      # 공통 사용 js 모음
│       │           ├── footer.html        # 푸터
│       │           ├── head.html          # head 공통 요소: 폰트, adminlte css, fontawesome 등 포함
│       │           ├── header.html        # 헤더
│       │           ├── sidebar.html       # 사이드바
│       │           └── template.html      # ⭐ 관리자용 화면 템플릿 (이 파일을 이용하여 다른 파일을 만들어주세요)
```

## 트러블슈팅 (문제 해결)

### Q 클래스 생성 시 발생한 문제 해결법

VSCode cmd 터미널에서 자바 버전 확인: java -version
(17.0.17)
.\mvnw.cmd -version
Maven이 보고 있는 Java version 확인. 17이 아닌 경우 수정 필요

1단계 — Java 17 경로 확인
C:\Program Files\Java\jdk-17

2단계 — 환경변수 변경

1. 윈도우 검색 → 환경 변수 편집 열기
2. 시스템 변수에서 JAVA_HOME 찾기
3. 값을 Java 17 경로로 변경(C:\Program Files\Java\jdk-17)
4. Path 변수에서 %JAVA_HOME%\bin 있는지 확인

3단계 — 터미널 재시작 후 확인
bash.\mvnw.cmd -version
Java version: 17.x.x 로 바뀌었는지 확인

터미널에 입력:
.\mvnw.cmd compile -f "d:\DEV06\ict06_team1_finalPJ\pom.xml"
오류 없으면 빌드 성공

생성된 Q클래스 파일 확인
프로젝트 루트 > target > generated-sources > Q...Entity.java (java 파일로 직접 생성한 dto/엔티티 개수만큼 자동생성되어 있음)

dto(Entity) 수정 시 반드시 rebuild, pom.xml 컴파일
IntelliJ > 상단메뉴 Build > Rebuild Project
VSCode > 터미널 cmd > .\mvnw.cmd clean compile -f "d:\DEV06\ict06_team1_finalPJ\pom.xml"

둘 다 오류 없이 성공해야 프로젝트가 정상적으로 실행됨