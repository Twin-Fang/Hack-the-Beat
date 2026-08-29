# Antigravity 101 — 세션 자료 분석

> 출처: `[IOEX26] Antigravity 101 - 참가자 공유용.pdf` (26쪽)
> 행사: 2026 I/O Extended: Hack the Beat (GDG Campus Korea)
> 발표: 김대현 — Solutions Architect (AI/ML, Google Cloud) @ MegazoneSoft, GDG Campus Korea Organizer
> 원본 슬라이드: https://docs.google.com/presentation/d/1mfLX8bbLfpB4Kuxcj_AtX2NJdvhhOQIxpu8A4rv_A6U/edit
> Ralph Loop 상세는 [ralph-loop.md](./ralph-loop.md) 참고

## 한 줄 요약

2026 Google I/O에서 공개된 **Antigravity 2.0**과 헤드리스 CLI **`agy`** 사용법, 기존 Agent/Skill을 받아 쓰고 직접 만드는 법, 그리고 Antigravity로 **Ralph Loop**(자율 반복 코딩)를 돌리는 방법을 다룬다. 핵심 메시지는 "Antigravity를 프롬프트 질의용으로만 쓰지 말고 Skill·Agent·Loop로 확장해서 써라".

---

## 1. Antigravity 포지셔닝 (vs 다른 코딩 에이전트)

슬라이드 3쪽의 비교 내용 그대로 정리.

### 강점

| 항목 | 내용 |
|---|---|
| 가성비 | 토큰 사용량이 약 **1/4** (Gemini 3.7 Flash 기준, Sonnet 5 대비) |
| 속도 | 초당 output 속도 **3배** (Gemini 3.7 Flash 기준, GPT-5.6 Sol high 대비) |
| 학생 혜택 | **Gemini for Students** — Google AI Plus 요금제 무료. Gemini 외에 Claude Sonnet·Opus 4.6, GPT-OSS 모델도 사용 가능 |
| 원스톱 | 앱 프로토타이핑 → CLI 반복 → SDK 배포까지 한 플랫폼에서 끝나는 사이클 |

### 약점

| 항목 | 내용 |
|---|---|
| 커스터마이징 | 코딩 에이전트 커스터마이징(설정 파일·skill·hook 생태계)은 타 도구가 우위. Antigravity는 커스텀 생태계가 아직 약함 |
| 클라우드 위임 | 클라우드 작업·Slack 등 서드파티 연동, 공개 CLI를 API처럼 쓰는 유연함은 타 도구가 앞섬. Antigravity는 아직 **Local 중심** |

> 시사점: 해커톤처럼 **빠르게 많이 돌려야 하는 상황**에서는 가성비·속도 강점이 크고, 부족한 커스텀 생태계는 외부 skill 레포를 플러그인으로 가져와 보완한다 (3절).

---

## 2. 제품 구성과 설치

### 제품군 (antigravity.google/download)

- **Antigravity 2.0** (세션 시점 v2.8.1) — 데스크톱 앱 (macOS Apple Silicon / Windows x64 / Linux x64)
- **Antigravity CLI** (`agy`) — 헤드리스 CLI. 이 세션의 주인공
- **Antigravity IDE**
- **Antigravity SDK**

### CLI 설치 & 헬스체크

```bash
# macOS / Linux
curl -fsSL https://antigravity.google/cli/install.sh | bash
# → ~/.local/bin/agy

agy models          # 사용 가능 모델 확인
agy -p "reply OK"   # 헤드리스 단발 실행 (health check)
agy                 # 대화형 실행
```

- 첫 실행 시 로그인 방식 선택: **Google OAuth** 또는 **Google Cloud 프로젝트** 연결
- 다운로드 페이지에서 "Antigravity CLI" 탭 → 장비에 맞는 버전 설치

### 설정 위치

| 범위 | 경로 | 용도 |
|---|---|---|
| 레포 단위 | `.agents/` | 레포에 커밋하면 **팀 전체 공유** |
| 글로벌 | `~/.gemini/config/` | 내 모든 프로젝트에 적용 |

---

## 3. 기존 Agent & Skill 받아와서 쓰기

### 플러그인 설치

스킬 모음 큐레이션: https://github.com/sickn33/agentic-awesome-skills

```bash
# 방법 1 — 레포 URL로 직접 설치
agy plugin install https://github.com/addyosmani/agent-skills.git

# 방법 2 — 로컬 clone 후 설치
git clone https://github.com/addyosmani/agent-skills.git
agy plugin install ./agent-skills

# 설치 확인
ls -l ~/.gemini/antigravity-cli/plugins/agent-skills/skills/
```

설치 결과 예시 (`addyosmani/agent-skills`): skills 24개 · agents 4개 · commands 8개(스킬로 변환) · mcpServers/hooks는 없어서 skip.

포함된 스킬 예: `api-and-interface-design`, `browser-testing-with-devtools`, `ci-cd-and-automation`, `code-review-and-quality`, `code-simplification`, `context-engineering`, `debugging-and-error-recovery`, `documentation-and-adrs`, `frontend-ui-engineering`, `git-workflow-and-versioning`, `incremental-implementation`, `performance-optimization`, `planning-and-task-breakdown`, `security-and-hardening`, `shipping-and-launch`, `spec-driven-development`, `test-driven-development` 등.

### 호출

- 대화형 세션에서 `/agents` 입력 → 설치된 에이전트/스킬 목록이 뜨고 선택해서 실행
- 직접 호출: `/agent-skills:code-review-and-quality 현재 코드 상태 보고 리뷰해줘`
- 시연 예: 코드 리뷰 스킬이 레포 파일을 훑고 **리뷰 리포트 아티팩트**(Required / Consider / Nit 분류, 위치·내용 표)를 생성

---

## 4. Agent & Skill 직접 만들기

두 가지 방법:

1. 만들고 싶은 스킬을 **`SKILL.md`** 로 직접 작성해 `.gemini/skills/<이름>/` 폴더에 넣기
2. **AGY에게 md 파일을 붙여넣고 "이런 에이전트 만들어줘"** 라고 지시 (더 빠름)

### SKILL.md 형식 (예시: `tpm-doc-ko`)

```markdown
---
name: tpm-doc-ko
description: TPM 산출물을 한국어로 개조식으로 작성·검토한다. 킥오프 문서, 요구사항 정의서, 의사결정 기록, 회의록, 상태 보고 …
tools: Read, Write, Edit, Glob, Grep
model: sonnet
---

당신은 TPM(Technical Program Manager) 문서 작성자다. …
## 최우선 원칙
…
## 문서 유형 라우팅
…
```

- frontmatter에 `name` / `description` / `tools` / `model`
- 본문에 역할·원칙·라우팅 규칙을 개조식으로
- AGY가 생성하면 `~/.gemini/config/agent-skills/skills/<이름>/SKILL.md` 위치에 저장되고, 이후 `/tpm` 처럼 슬래시 명령으로 호출 가능
- 시연: `/tpm` 으로 README를 검토시켜 "섹션 헤딩 명사형 통일, 종결어미 통일, 제약·비교 항목 표로 확장, 보안 고려사항 추가" 같은 변경 전/후 표를 받음

---

## 5. Ralph Loop — 요약

> 상세: [ralph-loop.md](./ralph-loop.md)

- **정의**: 코딩 에이전트에게 작업을 맡긴 뒤 "구현 → 테스트 → 실패 분석 → 수정"을 사람 개입 없이 **테스트 통과까지 while 문으로 무한 반복**시키는 자동화 패턴
- **원조**: `while :; do cat PROMPT.md | amp --dangerously-allow-all; done`
- **Agy 버전 6단계**: PRD.md에서 태스크 읽기 → progress.txt로 진행 확인 → **딱 1개** 태스크 완료 → progress append(삭제 금지) → commit → 전부 끝나거나 최대 반복까지 반복
- **Antigravity에서 특별히 신경 쓸 점**
  - shell cwd를 워크스페이스로 상속하지 않음 → 절대경로를 `--add-dir`로 넘기고 프롬프트에도 박아야 함
  - 세션 유지가 기본 설계(`--continue`)인데 Ralph는 **세션을 죽여야** 함 → `agy -p` 단발 호출만 사용
  - 멈출 수 있는 지점 전부 제거 → `--mode accept-edits --dangerously-skip-permissions --print-timeout 15m`
  - 모델명은 UI 표시 문자열을 따옴표로 → `RALPH_MODEL="Gemini 3.1 Pro (High)"`
- **구성요소**: `PRD.md`(Task + 운영 규칙) / `progress.txt`(append-only 로그) / `ralph.sh`(루프 러너) / `.git/`(commit log 필수)

---

## 6. 발표자의 해커톤 팁

### Status는 파일과 git에, Context에 두지 마라

- 진행 상황은 LLM 컨텍스트 윈도우가 아니라 **파일 + git 히스토리**로 관리하는 게 Ralph의 핵심
- `PROMPT.md` + `fix_plan.md`(또는 `progress.md`)를 Loop가 무조건 읽게 두기
- **매 이터레이션마다 커밋 강제** → Loop가 폭주해도 git으로 복구 가능 (안전장치)
- 최대 이터레이션 수로 안전장치 (토큰은 소중하니까)

### Prompt를 잘 쓰고, 종료 조건을 잘 판단하자

- Loop 돌릴 때 LLM은 md 파일의 요구사항만 보고 일하므로 **요구사항 품질 = 산출물 품질**
- `PROMPT.md`의 "완료 조건(Definition of Done)"을 **테스트 가능한 문장**으로 쓸 것
  - ❌ "로그인 되게 해줘"
  - ✅ "`npm test` 통과 + `/login` POST 200 반환"
- 테스트 스위트, 빌드 성공처럼 **판정 가능한 조건을 탈출 조건**으로

---

## 7. 참고 링크

| 항목 | URL |
|---|---|
| Antigravity 다운로드 | https://antigravity.google/download |
| 스킬 큐레이션 | https://github.com/sickn33/agentic-awesome-skills |
| 예시 스킬 팩 | https://github.com/addyosmani/agent-skills |
| Ralph Loop 원문 | https://www.thetoolnerd.com/p/autonomous-ai-agent-loop-for-building |
| Ralph for Antigravity (참고 구현) | https://github.com/abhishekbhakat/ralph-loop-for-antigravity |
| 세션 테스트 레포 | https://github.com/GDGCampusKorea/antigravity-ralph-loop-test |
| 행사 페이지 | https://event-us.kr/gdgcampuskorea/event/131744 |

---

## 8. Hack-the-Beat 프로젝트 적용 포인트

- **스킬 팩 설치**: `agy plugin install https://github.com/addyosmani/agent-skills.git` 한 줄로 코드리뷰·TDD·프론트엔드 스킬 확보
- **레포 공유 설정**: 팀이 같이 쓸 스킬/에이전트는 `.agents/`에 두고 커밋
- **Ralph Loop로 기능 구현**: 아이템 확정 후 `PRD.md`에 태스크를 "한 세션에 끝나는 크기"로 쪼개고 `Done when`에 `npm run build && npm run lint` 통과 같은 판정 가능 조건을 넣어 돌린다. 세팅 방법은 [ralph-loop.md](./ralph-loop.md) 하단 참고
- **완료 조건은 검증 가능하게**: "화면 예쁘게" 대신 "빌드 통과 + 해당 라우트 렌더링 + 콘솔 에러 0"
