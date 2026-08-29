# Hack-the-Beat 작업 가이드

해커톤 프로젝트. **속도 최우선** — 설계 문서·스펙 파일 없이 바로 구현하고, 질문은 진짜 갈림길일 때만 한 번에 하나.

## 스택

- React 19 + TypeScript + Vite 8 (`npm run dev` → http://localhost:5173)
- Tailwind CSS v4 + daisyUI v5 — 스타일은 daisyUI 클래스(`btn`, `card`, `modal` …) 우선, 커스텀 CSS 최소화
- TanStack Query v5 — 서버 상태 전부 (`src/lib/queryClient.ts`)
- Zustand v5 — 전역 클라이언트 상태 (`src/stores/`)
- React Router v8 — 선언형 `<Routes>` (`src/App.tsx`), 가드는 `src/components/RequireAuth.tsx`

## 디렉토리

```
src/
  pages/        라우트 단위 화면 (XxxPage.tsx)
  components/   재사용 컴포넌트
  stores/       Zustand 스토어 (useXxxStore.ts)
  lib/          설정·유틸 (queryClient 등)
```

## 코드 규칙

- 조건부 렌더링은 삼항 연산자. `{cond && <X/>}` 금지 — `0`, `""`이 그대로 렌더링됨
- 로딩/에러/성공은 `isPending ? … : isError ? … : …` 패턴
- API 연결 시 `useQuery`/`useMutation`만 사용, 컴포넌트에서 직접 fetch 금지
- 주석은 한국어, WHY 중심으로 짧게. 도구·작성자 언급 금지
- 새 페이지 추가 = `pages/`에 파일 생성 + `App.tsx`에 `<Route>` 한 줄

## 작업 흐름

1. 구현 → `npm run build` + `npm run lint` 통과 확인
2. 커밋 — `main` 직접. 메시지 형식: `{작업 제목} : {feat|fix|chore|docs|refactor} : {변경 요약}`
   - `Co-Authored-By`, 서명, 트레일러, 도구 언급 **절대 금지** — 작성자 git 설정만 사용
   - 스테이징은 내가 건드린 경로만 명시 (`git add -A` 금지)
3. push 전 `git pull --rebase origin main` — 버전 봇 커밋(`[skip ci]`)이 수시로 생김
4. `main` push → GitHub Actions가 GitHub Pages로 자동 배포 (약 30초)
   - 배포 주소: https://twin-fang.github.io/Hack-the-Beat/
   - 빌드 시에만 `base: /Hack-the-Beat/` 적용, dev는 `/`
5. 배포 후 URL 응답 확인까지 하고 완료 보고

## 심사 대응 (상세: docs/judging-criteria.md — 가장 중요한 문서)

- 주제 **"Make the Party Better"** — 파티(모임 전·중·후)가 제품의 핵심 전제여야 배율 1.0. 단톡방·SNS로 대체되면 감점
- AI 심사관이 **Playwright로 배포 URL을 직접 조작**한다. 제출 시나리오는 **3단계**. 버튼·라벨·완료 텍스트는 시나리오와 **글자 단위로 동일**
- 3단계 안에 **초대/공유/동반 참여 동작을 포함** (B3 30% — 브라우저에서 실동작해야 상한 해제). 리텐션 트리거도 화면에서 확인 가능하게
- 핵심 플로우는 **로그인 없이** 시작, 완료 시 명시적 완료 텍스트 + URL 변화. **데스크톱·모바일 모두** 콘솔 에러 0
- 인터랙티브 요소는 시맨틱 태그(`<button>`, `<a>`, `<form>`) + `aria-label` + `data-testid`. `<div onClick>` 금지
- 빈 상태·에러·로딩 화면 처리, 더미 텍스트 금지. 핵심 플로우를 외부 API·결제·실시간 서버에 의존시키지 않는다. 심사 중 배포 프리즈
- 기획안·스크립트에는 **실제 동작하는 기능만** 쓴다 (미구현 기능 근거는 6점 상한)
- 제출 폼 필드·글자 제한·템플릿은 docs/submission-guide.md. 테스트 계정은 비우는 게 원칙이라 **스캐폴드의 `RequireAuth` 로그인 가드는 실제 제품에서 제거**한다

## 건드리지 말 것

- `.github/workflows/PROJECT-COMMON-*.yaml`, `.github/scripts/`, `version.yml`, `.github/.wizard/` — 자동화 마법사 관리 영역. 버전은 워크플로우가 올림
- `README.md` 하단 `AUTO-VERSION-SECTION` 블록 — 워크플로우가 갱신
- `.agents/`, `.gstack/` — 로컬 도구 산출물. 커밋 금지 (`.git/info/exclude`로 제외됨)
- 파일 삭제는 확인 후에만
