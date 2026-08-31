# 회고록 / Retrospective

2026 I/O Extended: Hack the Beat 우승 회고.

| 언어 | 파일 |
|---|---|
| 🇰🇷 한국어 | [ko.md](./ko.md) |
| 🇺🇸 English | [en.md](./en.md) |

---

**한 줄 요약** — 심사위원이 AI Agent이고 Playwright가 배포 링크를 직접 조작해 평가하는 구조였다. 제품을 만들기 전에 채점기를 역설계하고, 아이디어 7개를 그 채점기에 병렬로 돌려 최고점 아이디어를 채택했다. 화면의 첫 사용자를 Playwright로 두고 설계했고, 제출 직전 자가 채점으로 15건을 고쳤다.

**Summary** — The judges were AI agents, and Playwright drove the deployed link to evaluate whether the product worked. We reverse-engineered the grader before building, scored seven candidate ideas against it in parallel, and took the highest. We designed the UI treating Playwright as its first user, and fixed 15 issues through self-assessment before submitting.
