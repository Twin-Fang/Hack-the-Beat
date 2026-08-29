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

## 건드리지 말 것

- `.github/workflows/PROJECT-COMMON-*.yaml`, `.github/scripts/`, `version.yml`, `.github/.wizard/` — 자동화 마법사 관리 영역. 버전은 워크플로우가 올림
- `README.md` 하단 `AUTO-VERSION-SECTION` 블록 — 워크플로우가 갱신
- `.agents/`, `.gstack/` — 로컬 도구 산출물. 커밋 금지 (`.git/info/exclude`로 제외됨)
- 파일 삭제는 확인 후에만
