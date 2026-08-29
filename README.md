# Hack-the-Beat

2026 I/O Extended: Hack the Beat 출품작 — 주제 **"Make the Party Better"** (파티를 더 잘 즐길 수 있는 서비스).

🔗 **배포 주소**: https://twin-fang.github.io/Hack-the-Beat/

## 문서 — 먼저 읽을 것

| 문서 | 내용 |
|---|---|
| [docs/judging-criteria.md](docs/judging-criteria.md) | **공식 채점 루브릭** 12항목 앵커·상한 규칙·주제 게이트 배율, 주제 분석과 아이템 선정 필터, Playwright 대응 규칙 |
| [docs/submission-guide.md](docs/submission-guide.md) | **제출 페이지 실물** 기준 필드별 작성법, 글자 제한(기획안 8,000 / 스크립트 4,000), 3단계·기획안·스크립트 템플릿, 직전 체크리스트 |
| [docs/personas/](docs/personas/README.md) | AI 심사관 3명(창업가·엔지니어·투자자) **추정 페르소나** — 렌즈별 질문·근거·감점 트리거·자가 점검표·가채점표 |
| [AGENTS.md](AGENTS.md) | 작업 규칙 — 스택, 디렉토리, 코드 규칙, 커밋·배포 흐름, 심사 대응 |
| [docs/antigravity-101.md](docs/antigravity-101.md) · [docs/ralph-loop.md](docs/ralph-loop.md) | 세션 자료 정리 (참고용) |

## 심사 핵심 3줄

1. AI 심사관 3명이 12항목 × 3회 채점. **A(34%)는 Playwright가 배포 URL을 직접 조작한 결과만** 본다 — 제출 시나리오 3단계가 그대로 실행된다.
2. **총점 = 주제 게이트 배율 × (0.34·A + 0.33·B + 0.33·C)**. 파티(모임 전·중·후)가 핵심 전제여야 ×1.0.
3. B·C는 **숫자·고유명사·관찰**이 있어야 5점을 넘고, 문서에 쓴 기능이 앱에서 확인돼야 6점을 넘는다. 제출은 1회, 수정 불가.

## 기술 스택

- React 19 + TypeScript + Vite 8
- Tailwind CSS v4 + daisyUI v5
- TanStack Query (서버 상태) · Zustand (전역 상태) · React Router v8

## 실행

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # dist/ 생성
```

## 배포

`main` 브랜치에 push하면 GitHub Actions가 빌드 후 GitHub Pages로 자동 배포한다. 심사 제출 후에는 push하지 않는다(배포 프리즈).

---

<!-- AUTO-VERSION-SECTION: DO NOT EDIT MANUALLY -->
## 최신 버전 : v0.0.4 (2026-08-29)

[전체 버전 기록 보기](CHANGELOG.md)
