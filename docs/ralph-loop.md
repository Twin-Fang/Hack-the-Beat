# Ralph Loop — Antigravity(`agy`)로 자율 반복 코딩 돌리기

> 출처
> - GDG Campus Korea 세션 테스트 레포: https://github.com/GDGCampusKorea/antigravity-ralph-loop-test (MIT, 작성자 [@Daehyun-Bigbread](https://github.com/Daehyun-Bigbread))
> - 세션 슬라이드 [Antigravity 101](./antigravity-101.md) 14~24쪽
> - Ralph Loop 원문: https://www.thetoolnerd.com/p/autonomous-ai-agent-loop-for-building

## 1. Ralph Loop이란

코딩 에이전트에게 작업을 맡긴 뒤 **"구현 → 테스트 → 실패 원인 분석 → 코드 수정"** 과정을 사람이 개입하지 않고 테스트를 통과할 때까지 **while 문으로 무한 자율 반복**시키는 자동화 패턴.

### 해결하려는 LLM의 근본 문제 2가지

1. **컨텍스트 창 한계** — 작업 도중 중요한 맥락을 잊어버림
2. **지속적인 감독 필요** — 오래 자율로 일하지 못함

### 해결책

- **메모리를 파일로 외부화** (`PRD.md`, `progress.txt`, git log)
- **매 반복마다 새 세션(fresh context)** 으로 에이전트를 다시 띄움

즉 매 반복이 "죽었다 새로 뜨는 세션"이고, 상태는 대화 메모리가 아니라 **디스크에서 매번 다시 읽는다**. 그래서 컨텍스트가 새어 나가지 않는다.

### 핵심 동작 원리

1. **명령 입력**: 에이전트에게 구체적인 기능 요구사항과 평가 기준(테스트 코드)을 전달
2. **코드 생성·실행**: 에이전트가 코드를 작성/수정
3. **결과 검증**: 정해진 검증 방식(`pytest`, `npm test` 등 자동화 스크립트) 실행
4. **자율 피드백 루프**
   - Pass → 루프 종료
   - Fail → 에러 로그·스택 트레이스를 다시 컨텍스트로 주입하고 1번으로

### 원조 형태

```bash
while :; do cat PROMPT.md | amp --dangerously-allow-all; done
```

---

## 2. Agy 버전 6단계

원조는 "완료 문자열이 안 나옴" 하나만 보는 해피패스라, 안정적으로 돌리기 위해 6단계로 재구성 (완전한 Ralph는 아님):

```
1. PRD.md 에서 태스크를 읽는다
2. progress.txt 로 진행 상황을 확인한다
3. 딱 1개 태스크만 완료한다          ← "딱 1개"가 핵심
4. progress 를 append 한다           ← 절대 삭제 금지
5. 커밋한다
6. 전부 끝나거나 최대 반복 횟수에 도달할 때까지 반복
```

> ⚠️ `agy`의 대화형 세션이나 `--continue` / `--conversation`은 컨텍스트를 이어붙이므로 **Ralph Loop이 아니다** (anti-Ralph). 반드시 반복적 `agy -p` 호출로 돌린다.

---

## 3. 왜 Antigravity에서는 이렇게 돌려야 하나

### (1) cwd를 워크스페이스로 상속하지 않는다

Antigravity는 shell의 cwd를 워크스페이스로 쓰지 않는다. 프롬프트에 절대 경로를 안 박으면 엉뚱한 곳에 산출물을 만든다. (다른 CLI 도구는 `cd repo && ralph.sh`로 끝나지만 `agy`는 안 통함)

```bash
REPO="$(cd "$(dirname "$0")" && pwd)"
...
--add-dir "$REPO"
```

프롬프트에도 명시:

```
The project directory is: $REPO
All file reads, file writes, and git commits MUST happen
inside that exact directory.
```

### (2) 세션을 이어가는 게 기본 설계

Antigravity는 에이전트 매니저가 전면에 있는 제품이라 세션 유지(`--continue`)가 기본. 그런데 Ralph는 **세션을 죽여야** 한다 (전제 충돌). 그래서 `agy -p` 단발 호출만 쓰고, 컨텍스트는 `PRD.md + progress.txt + git log`로 보존한다.

```bash
# ❌ Anti-Ralph — 세션이 살아있음
agy --continue

# ✅ Ralph — 매 반복 새 세션
agy -p "$PROMPT" --add-dir "$REPO"
```

### (3) 멈출 수 있는 지점을 전부 제거

Ralph는 사람 개입 없이 N회 반복되는 게 전제라, 한 반복이라도 사람을 기다리며 정지하면 루프 전체가 죽는다. `agy`는 실행 모드(`accept-edits`)와 권한 승인(`skip-permissions`)이 분리돼 있어 **둘 다** 줘야 한다. 원조의 `--dangerously-allow-all` 하나에 대응하는 게 `agy`에서는 두 개.

```bash
--mode accept-edits --dangerously-skip-permissions
--print-timeout 15m
```

### (4) 모델명은 UI 표시 문자열을 따옴표로

Antigravity 안에서 여러 벤더 모델을 같은 루프·같은 PRD로 돌릴 수 있다.

```bash
RALPH_MODEL="Gemini 3.1 Pro (High)" bash ralph.sh
bash ralph.sh   # 기본값
```

---

## 4. 구성요소

```
PRD.md          # Task + Agent 동작 규칙 (단일 진실 공급원)
progress.txt    # append-only 로그
ralph.sh        # 루프 러너
.git/           # 필수 — commit log
```

| 파일 | 역할 |
|---|---|
| `PRD.md` | 단일 진실 공급원. 태스크 목록 + 에이전트 운영 규칙 + 디자인 방향. 에이전트가 **매 반복 이 파일을 다시 읽는다** |
| `progress.txt` | append-only 진행 로그. 완료 태스크마다 한 줄 추가, 전부 끝나면 `ALL TASKS COMPLETE` 기록 |
| `ralph.sh` | 루프 러너. `agy -p`를 매 반복 새 세션으로 호출하며 **실패 감지 + 정체(stall) 감지**로 안전하게 멈춤 |
| `TEST_PLAN.md` | 동일 PRD를 여러 모델로 돌려 비교하기 위한 브랜치 전략·평가 기준 |

### PRD 작성 원칙 (태스크 규모 무관 공통)

1. 태스크마다 **검증 기준(`Done when:`)** 을 붙인다. 없으면 에이전트가 자기 기준으로 완료 처리해버림
2. **운영 규칙을 프롬프트가 아니라 PRD 안에** 둔다. 프롬프트에만 있으면 새 세션이 요약본만 받지만, PRD에 있으면 매 반복 원문을 다시 읽음
3. 태스크는 **한 세션에 끝나는 크기**로 쪼갠다. 세션 경계를 넘는 태스크는 루프에서 계속 돌 수 있음
4. **암묵적 맥락을 전부 문서화**한다

---

## 5. 루프 베이스 (다른 프로젝트로 이식할 때 이 형태 유지)

```bash
REPO="$(cd "$(dirname "$0")" && pwd)"          # 절대경로 확보
for i in $(seq 1 "$MAX_ITERS"); do
  before=$(진전지표 측정)                      # 체크 수 + git HEAD
  agy -p "$PROMPT" --model "$MODEL" --add-dir "$REPO" \
      --mode accept-edits --dangerously-skip-permissions --print-timeout 15m
  rc=$?
  after=$(진전지표 측정)
  센티널 있으면 → 정상 종료
  rc / (after-before) 로 판정 → stall 카운트
  연속 N회 stall → abort
  백오프
done
```

---

## 6. 테스트 레포 상세 (`antigravity-ralph-loop-test`)

### 목표 산출물

`PRD.md`에 정의된 **의존성 없는 정적 랜딩 페이지** (순수 HTML/CSS/JS, 빌드 스텝 없음, `index.html` 더블클릭으로 열림).

### 요구 사항

- macOS + Antigravity 설치 (헤드리스 CLI `agy` 포함), `agy`가 PATH에 있을 것 (예: `~/.local/bin/agy`)
- git 레포 (커밋이 발생하므로)

```bash
agy models          # 사용 가능한 모델 목록
agy -p "reply OK"   # 헤드리스 단발 실행 테스트
```

### 실행

```bash
cd ~/Documents/GitHub/antigravity-ralph-loop-test
bash ralph.sh
```

가장 번호가 낮은 미완료 태스크부터 하나씩 완료 → `[x]` 체크 → `progress.txt` append → 커밋 → `ALL TASKS COMPLETE`가 나오면 exit 0. 연속 정체/에러가 `RALPH_MAX_STALLS`회에 도달하거나 `RALPH_MAX_ITERS`를 다 쓰면 exit 1 (재실행하면 이어서 진행). 중간에 멈추기: `Ctrl+C`.

### 환경변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `RALPH_MODEL` | `Gemini 3.1 Pro (High)` | 사용할 모델 |
| `RALPH_MAX_ITERS` | `15` | 최대 반복 횟수 (안전 상한) |
| `RALPH_MAX_STALLS` | `2` | 연속 무진전/에러 몇 번이면 중단할지 |

```bash
RALPH_MODEL="Claude Sonnet 4.6 (Thinking)" RALPH_MAX_ITERS=20 bash ralph.sh
```

> 디자인 품질(generic 템플릿 금지, editorial-Swiss, oklch 토큰, 접근성)을 요구하는 PRD라 Flash급 모델은 결과가 밋밋할 수 있음. Sonnet 4.6 (Thinking) 또는 Gemini 3.1 Pro (High) 권장.

### `ralph.sh`의 안전장치

- **실패 감지** — `agy` 종료 코드 확인. 크래시/타임아웃/레이트리밋이면 "무진전"으로 분류 (조용히 성공 처리하지 않음)
- **정체(stall) 감지** — 반복 전후로 `PRD.md`의 `[x]` 개수를 비교해 하나도 안 늘었으면 정체. git HEAD도 별도 비교해 "태스크는 늘었는데 커밋이 없는" 커밋 위생 문제를 로그로 알림
- **연속 정체 시 중단** — `RALPH_MAX_STALLS`번 연속이면 즉시 중단. 같은 태스크에 막혀 `MAX_ITERS`를 전부 태우는 상황 방지
- **실패 시 백오프** — 정체/에러 뒤 대기 시간을 늘려 레이트리밋 회복 (진전 시 2초, 정체 시 15초)

### ⚠️ 주의

`--dangerously-skip-permissions`는 임의 도구 실행을 무제한 승인한다. 테스트 레포에서는 괜찮지만 실제 프로젝트에서는 **별도 git worktree나 격리 디렉터리**에서 돌리고, 문제 시 `git reset --hard`로 복구할 것.

---

## 7. `PRD.md` 구조 (실제 파일 기준)

```markdown
# PRD — Static Landing Page (Ralph Loop Test)

## Goal
(무엇을 만드는지 + "This file is the single source of truth. The agent reads it every iteration.")

## Agent Operating Rules (Ralph Loop)
- Complete exactly ONE unchecked task per iteration (the lowest-numbered `[ ]`).
- After finishing, mark it `[x]` here, then append a line to `progress.txt`
  (format: `TASK <n> DONE — <one-line summary>`). Never delete progress history.
- Commit the changes for that single task with message `feat: task <n> — <summary>`.
- Verify each task against its Done when criteria before marking complete.
- Do NOT start the next task in the same iteration. Stop after one.
- If all tasks are `[x]`, append `ALL TASKS COMPLETE` to `progress.txt` and stop.

## Design Direction
(스타일 방향, 팔레트 토큰, 타이포그래피, Non-negotiables)

## Target Structure
(생성될 파일 트리)

## Tasks
- [ ] 1. … **Done when:** …
- [ ] 2. … **Done when:** …
  … (총 11개)

## Definition of Done (whole project)
(전체 완료 조건)
```

테스트 레포의 11개 태스크: ① index.html 골격 → ② tokens.css → ③ global.css(리셋·reduced-motion) → ④ typography.css(웹폰트·clamp 스케일) → ⑤ header/nav → ⑥ hero → ⑦ features/bento → ⑧ footer → ⑨ 반응형(320/375/768/1024/1440) → ⑩ main.js 등장 애니메이션(IntersectionObserver) → ⑪ 접근성·폴리시 패스. 각 태스크에 **Done when** 판정 기준이 붙어 있다.

### `progress.txt` 초기 상태

```
# Ralph Loop progress log (append-only — never delete lines)
# Format: TASK <n> DONE — <one-line summary>
# The agent appends one line per completed task, then commits.
```

---

## 8. `ralph.sh` 전문

다른 프로젝트로 이식할 때 그대로 복사해서 `PRD`/`PROGRESS` 경로와 프롬프트만 바꾸면 된다. (MIT)

```bash
#!/usr/bin/env bash
# Ralph Loop runner for Antigravity (agy CLI)
# Each iteration = a FRESH agy session that re-reads PRD.md + progress.txt from disk,
# does exactly ONE task, marks it [x], appends progress, and commits.
#
# Hardened control loop:
#   - FAILURE detection : inspects agy's exit code; a crash/timeout/rate-limit is
#                         treated as a non-progress iteration, not a silent success.
#   - STALL detection   : snapshots checked-off task count (and git HEAD) before/after
#                         each iteration. If no task got completed, it's a stall.
#                         Aborts after RALPH_MAX_STALLS consecutive non-progress iters,
#                         instead of burning the whole MAX_ITERS budget on a stuck task.
set -uo pipefail
# Absolute path to this repo — agy does NOT inherit the shell cwd as its workspace,
# so we pass it explicitly via --add-dir AND bake it into the prompt.
REPO="$(cd "$(dirname "$0")" && pwd)"
cd "$REPO"

MODEL="${RALPH_MODEL:-Gemini 3.1 Pro (High)}"
MAX_ITERS="${RALPH_MAX_ITERS:-15}"
MAX_STALLS="${RALPH_MAX_STALLS:-2}"   # consecutive no-progress/error iters before abort
PRD="PRD.md"
PROGRESS="progress.txt"

# Count only real checkbox task lines ("- [x] ..."), not literal `[x]` inside prose.
count_done() { grep -cE '^- \[x\]' "$PRD" 2>/dev/null || true; }

# First (double-quoted) chunk injects the absolute path; second (single-quoted) chunk
# keeps the literal double-quotes in the rules intact.
PROMPT="The project directory is: $REPO
All file reads, file writes, and git commits MUST happen inside that exact directory.
cd into it first, then read ./PRD.md and ./progress.txt there.
"'You are running ONE iteration of a Ralph Loop.
Follow the "Agent Operating Rules (Ralph Loop)" section of PRD.md EXACTLY:
- Do the single lowest-numbered unchecked "[ ]" task only.
- Verify it against its "Done when" criteria.
- Mark that task "[x]" in PRD.md.
- Append one line to progress.txt: "TASK <n> DONE — <one-line summary>" (never delete lines).
- Commit ONLY this task with message "feat: task <n> — <summary>".
- Do NOT start a second task. Stop after one.
- If every task is already "[x]", append "ALL TASKS COMPLETE" to progress.txt and stop.'

stalls=0
for i in $(seq 1 "$MAX_ITERS"); do
  echo "───────────────────────────── Ralph iteration $i / $MAX_ITERS  (model: $MODEL)"

  before_head="$(git rev-parse HEAD 2>/dev/null || echo none)"
  before_done="$(count_done)"; before_done="${before_done:-0}"

  agy -p "$PROMPT" \
      --model "$MODEL" \
      --add-dir "$REPO" \
      --mode accept-edits \
      --dangerously-skip-permissions \
      --print-timeout 15m 2>&1
  rc=$?

  after_head="$(git rev-parse HEAD 2>/dev/null || echo none)"
  after_done="$(count_done)"; after_done="${after_done:-0}"

  # --- completion: sentinel wins regardless of everything else ---
  if grep -q "ALL TASKS COMPLETE" "$PROGRESS" 2>/dev/null; then
    echo "✅ All tasks complete — stopping after $i iteration(s)."
    exit 0
  fi

  # --- classify this iteration: errored / advanced / stalled ---
  if [ "$rc" -ne 0 ]; then
    stalls=$((stalls + 1))
    echo "❌ agy exited non-zero (rc=$rc). Non-progress iteration ($stalls/$MAX_STALLS)."
  elif [ "$after_done" -gt "$before_done" ]; then
    [ "$after_head" != "$before_head" ] \
      && echo "✔ progress: tasks $before_done → $after_done, new commit $after_head." \
      || echo "✔ tasks $before_done → $after_done, but NO new commit (check commit hygiene)."
    stalls=0
  else
    stalls=$((stalls + 1))
    echo "⚠ no task completed this iteration (done stayed at $after_done). Stall ($stalls/$MAX_STALLS)."
  fi

  # --- abort if stuck ---
  if [ "$stalls" -ge "$MAX_STALLS" ]; then
    echo "🛑 Aborting after $stalls consecutive non-progress/error iterations."
    echo "   Inspect:  git -C \"$PWD\" log --oneline -5 ;  git status ;  tail progress.txt"
    exit 1
  fi

  # backoff: brief on progress, longer after a failure/stall (helps with rate limits)
  if [ "$stalls" -gt 0 ]; then sleep 15; else sleep 2; fi
done

echo "⚠️  Reached MAX_ITERS=$MAX_ITERS without ALL TASKS COMPLETE. Re-run to continue."
exit 1
```

---

## 9. 멀티 모델 비교 (`TEST_PLAN.md` 요약)

동일 PRD를 여러 모델로 돌려 성능을 비교한다.

| # | 모델 | `--model` 값 | 상태 |
|---|---|---|---|
| 1 | Claude Sonnet 4.6 (Thinking) | `Claude Sonnet 4.6 (Thinking)` | 완료 (`run/claude-sonnet-4.6` 브랜치 보존) |
| 2 | Gemini 3.1 Pro (High) | `Gemini 3.1 Pro (High)` | 다음 (ralph.sh 기본값) |
| 3 | Gemini 3.5 Flash (High) | `Gemini 3.5 Flash (High)` | 이후 |

- **브랜치 전략**: baseline 커밋(PRD + ralph.sh + scaffold)에서 모델별 `run/<model>` 브랜치 분기, `progress.txt` 초기화 후 clean state에서 시작
- **정량 지표**: 완료율(/11), iteration 수, stall 횟수, 소요 시간, 커밋 수(태스크당 1커밋이 이상)
- **정성 지표**: 디자인 품질, PRD 준수도, 코드 품질(시맨틱 HTML·토큰·애니메이션 규칙), 반응형, 접근성, 커밋 위생
- 주의: 모델별 rate limit 차이 → stall 시 자동 backoff. Gemini 모델은 tool use 지원에 따라 동작이 다를 수 있어 첫 실행 모니터링 필수

---

## 10. 해커톤 팁 (슬라이드 23쪽)

- **Status는 파일과 git에** — 진행 상황은 컨텍스트 윈도우가 아니라 파일·git 히스토리로. 매 이터레이션 커밋 강제 → 폭주해도 git으로 복구. 최대 이터레이션으로 토큰 안전장치
- **Prompt와 종료 조건** — 요구사항 md 품질이 곧 산출물 품질. Definition of Done을 테스트 가능한 문장으로: "로그인 되게 해줘" ❌ → "`npm test` 통과 + `/login` POST 200 반환" ✅

---

## 11. Hack-the-Beat에 적용하기

이 레포는 React + Vite + TypeScript 프로젝트이므로 정적 페이지용 PRD를 그대로 쓸 수는 없고, 아래처럼 맞춘다.

### 세팅 절차

1. `ralph.sh`(8절)를 레포 루트에 복사
2. `progress.txt`를 초기 상태(7절)로 생성
3. `PRD.md` 작성 — 아래 템플릿

### React 프로젝트용 `PRD.md` 뼈대

```markdown
# PRD — <기능 이름>

## Goal
<한 문단. 무엇을 왜 만드는지>
This file is the single source of truth. The agent reads it every iteration.

## Agent Operating Rules (Ralph Loop)
- Complete exactly ONE unchecked task per iteration (the lowest-numbered `[ ]`).
- Before marking done, run `npm run build && npm run lint` and make sure both pass.
- After finishing, mark it `[x]` here, then append `TASK <n> DONE — <summary>` to `progress.txt`. Never delete lines.
- Commit ONLY this task: `<제목> : feat : <summary>`
- Do NOT start the next task in the same iteration.
- If all tasks are `[x]`, append `ALL TASKS COMPLETE` to `progress.txt` and stop.

## Project Conventions
- Stack: React 19, TypeScript, Vite, Tailwind v4 + daisyUI v5, TanStack Query, Zustand, React Router v8
- Pages in `src/pages/`, components in `src/components/`, stores in `src/stores/`
- Use daisyUI classes first. Conditional rendering with ternary only (no `&&`).
- Server state via TanStack Query only. Global client state via Zustand.
- See AGENTS.md for the full rule set.

## Tasks
- [ ] 1. <작업>  **Done when:** `npm run build` passes and `/route` renders `<Component>`.
- [ ] 2. <작업>  **Done when:** …

## Definition of Done (whole project)
All tasks `[x]`, `npm run build && npm run lint` pass, no console errors on every route.
```

### 실행

```bash
RALPH_MODEL="Gemini 3.1 Pro (High)" RALPH_MAX_ITERS=10 bash ralph.sh
```

### 주의

- 루프가 `main`에 직접 커밋하므로 **별도 브랜치나 worktree**에서 돌리고 끝난 뒤 검토·머지
- Done when에 "예쁘게" 같은 판정 불가 조건 금지 — 빌드/린트/라우트 렌더링/콘솔 에러 0처럼 스크립트로 확인 가능한 것만
- 태스크 하나 = 15분 타임아웃 안에 끝나는 크기
