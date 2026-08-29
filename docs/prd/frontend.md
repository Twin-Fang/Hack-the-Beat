# PRD — 파티 패스포트 · 프론트엔드

> 제품 정의·채점 근거는 [docs/thinking/파티패스포트.md](../thinking/파티패스포트.md) v3. 저장소·규칙·인증은 [backend.md](./backend.md).
> 스택: React 19 + TypeScript + Vite 8 + Tailwind v4 + daisyUI v5 + React Router v8 ([AGENTS.md](../../AGENTS.md)). 배포: GitHub Pages `https://twin-fang.github.io/Hack-the-Beat/`.

## 1. 목표와 우선순위

| 순위 | 목표 | 채점 근거 |
|---|---|---|
| 1 | **제출 3단계가 데스크톱·모바일에서 텍스트 그대로 완주** | A1 13.6% · B3 상한 해제 |
| 2 | 콘솔 에러 0, 빈/에러/로딩 상태, 더미 텍스트 0 | A2 6.8% · A4 5.1% |
| 3 | 기획안에 쓴 기능이 **전부** 화면에서 확인됨 (QR·코드 태그·증표·미션·종료·선택·결과·증표함·요금 문구) | A3 8.5% · 구현 대조 규칙 |
| 4 | 로그인 화면 없음, 모달·`alert`·호버 의존 0 | A1 |

**범위 밖(붙이지 않는다)**: 프로필·사진·채팅·연락처·푸시·로그인 UI·실시간 소켓·결제 연동·i18n·다크모드 수동 토글.

## 2. 사용자와 신원

- 역할은 **호스트 / 참가자** 둘. 호스트도 참가자 1번이다(생성 시 자동 등록). 호스트만 `파티 종료` 버튼을 본다.
- 신원 = 브라우저. 앱 시작 시 Firebase 익명 인증으로 `uid`를 받는다(화면 없음). 파티별 "나"는 `localStorage['me:<partyId>'] = { pid, code, name }`.
- **한 브라우저가 한 파티에 참가자를 여러 명 가질 수 있다.** 심사 에이전트가 탭 하나로 호스트→김서준을 연달아 하기 때문. 초대 링크(`/party/:id/join`)는 이미 참여했어도 **항상 참여 폼을 보여준다**. 마지막으로 참여한 사람이 "나"가 된다.
- 증표함 = `localStorage['passport']`, 파티를 넘어 누적. 기기 간 이전 없음.

## 3. 라우트와 화면

| 경로 | 화면 | 핵심 요소 (텍스트는 글자 단위 고정) |
|---|---|---|
| `/` | 첫 화면 | 제목 `파티 패스포트`, 한 줄 설명, **`<button>` `파티 만들기`** → `/new`, 요금 한 줄 `50명까지 무료 / 51명부터 파티당 9,900원`, 링크 `내 증표함` |
| `/new` | 파티 생성 | `<form>`: `파티 이름`(필수, 1~40자) · `내 이름`(선택, placeholder `호스트`, 비면 "호스트") · `인원`(number, 기본 20, 2~500) · 요금 한 줄 + 인원에 따라 `이 파티: 무료` / `이 파티: 9,900원` · 제출 `파티 만들기`. 성공 → `/party/:id` |
| `/party/:id` | 내 파티 화면 | 아래 3.1. 이 브라우저에 "나"가 없으면 `/party/:id/join`으로 replace |
| `/party/:id/join?from=CODE` | 참여 | 파티 이름, `from`이 있으면 `OO님의 초대` 안내, `<form>`: `이름`(필수) · 제출 `참여하기`. 이미 참여했으면 상단에 `이미 참여했어요 → 내 화면 보기` 링크(폼은 그대로). 성공 → `from` 있으면 `/party/:id/meet/CODE?joined=1`, 없으면 `/party/:id` (둘 다 `참여 완료` 배너) |
| `/party/:id/meet/:code` | 만남 확인 | 아래 3.2 |
| `/passport` | 내 증표함 | 파티별 그룹 → 증표 이름 · 획득 시각. 빈 상태: `아직 증표가 없어요. 파티에서 사람들을 만나보세요` + `파티 만들기` |
| `*` / 없는 파티 | 안내 | `파티를 찾을 수 없어요` + `홈으로` |

### 3.1 `/party/:id` 구성 (위→아래, 모바일 1열)

| 블록 | 내용 | 상태 |
|---|---|---|
| 헤더 | 파티 이름 · `N명 참여 중` · 호스트에게만 `초대 링크가 생성되었습니다` 배너(**새로고침해도 유지** — localStorage `host:<id>`) | — |
| InviteBox | 제목 `초대 링크`(호스트) / `내 초대 링크`(참가자). 읽기전용 `<input>`에 URL(= 내 코드가 실린 join 링크), `<button>` `초대 링크 복사` → 성공 시 `복사되었습니다` | 실패 시 input 전체 선택 + `링크를 길게 눌러 복사해 주세요`. 거짓 성공 금지 |
| MyQr | `<QRCodeSVG>`(`/party/:id/meet/<내 코드>` 절대 URL) · `내 코드: AB12` · 안내 `상대가 폰 카메라로 찍거나 코드를 입력하면 만남이 기록돼요` | — |
| Progress | `만난 사람 K명` · `<progress>` K/(N−1) | N=1이면 `아직 나뿐이에요. 초대 링크를 공유하세요` |
| TagByCode | `<form>`: `<label>` `코드로 태그` + 4자 `<input>`(대문자 자동) + `<button>` `태그` → `/party/:id/meet/CODE` | 없는 코드: `그 코드는 없어요` (meet 화면에서) |
| Mission | `미션 상대: OO` (+ `완료` 뱃지) | 짝 없음: `다음 참가자가 오면 정해져요` |
| Badges | 6개 카드(획득/잠금, 조건 문구) | 획득 즉시 증표함에 저장(중복 없이) |
| ParticipantList | 이름 · `나` · `호스트` · 만남 `✓` | — |
| 호스트 전용 | `<button>` `파티 종료` (클릭 1번, 다이얼로그 없음) | — |
| **종료 상태** (endedAt ≠ null 또는 createdAt+24h 경과) | 위 블록 대신: 미제출 → PickForm: `다시 만나고 싶은 사람` 체크박스(만난 사람만) + `선택 제출`. 제출 후 → MatchResult: `서로 선택된 사람` 목록 · `다시 확인` · `다음 파티 만들기` · `내 증표함` | 만난 사람 0: `이 파티에서 만난 사람이 없어요` + `다음 파티 만들기`. 결과 없음: `아직 서로 선택된 사람이 없어요. 상대가 제출하면 여기에 나타나요` |

### 3.2 `/party/:id/meet/:code` 만남 확인

| 상황 | 화면 |
|---|---|
| 이 브라우저에 "나"가 없음 | `/party/:id/join?from=CODE`로 replace (새 사람이 QR을 찍은 경우 = 참여 흐름) |
| `joined=1` | 상단 `참여 완료` 배너 |
| 코드가 내 것 | `내 코드예요. 다른 사람의 QR을 찍어 주세요` + `내 화면으로` |
| 코드 없음 | `그 코드는 없어요` + `내 화면으로` |
| 이미 만남 | `OO님과는 이미 만났어요` + `내 화면으로` |
| 파티 종료 | `종료된 파티예요` + `내 화면으로` |
| 정상 | `OO님을 만났나요?` + `<button>` **`지금 만났어요`** → 만남 생성 → `/party/:id` 로 이동, 토스트 `OO님과 만났어요 · 만난 사람 K명` |
| 다른 브라우저로 열림(신원 없음인데 참여는 이미 한 사람) | join 화면 상단 안내 `이미 참여했다면, 참여한 브라우저의 "코드로 태그"에 CODE를 입력하세요` |

## 4. 공통 요구사항

### 4.1 앱 부팅 (AuthGate)
- 루트에서 `signInAnonymously`. uid 확보 전: 전체 스피너. 실패: `연결에 실패했어요` + `<button>` `다시 시도`. **조용히 빈 화면 금지.**
- uid 확보 후에만 라우트 렌더. 익명 인증은 브라우저에 유지되므로 재방문 시 즉시 통과.

### 4.2 텍스트 고정표 (시나리오·기획안과 글자 단위 동일)
`파티 만들기` · `파티 이름` · `내 이름` · `인원` · `초대 링크가 생성되었습니다` · `초대 링크 복사` · `복사되었습니다` · `이름` · `참여하기` · `참여 완료` · `지금 만났어요` · `만난 사람` · `첫 만남` · `아이스브레이커` · `파티 피플` · `파티 마스터` · `미션 완료` · `재회` · `내 코드` · `코드로 태그` · `태그` · `미션 상대` · `파티 종료` · `다시 만나고 싶은 사람` · `선택 제출` · `서로 선택된 사람` · `다시 확인` · `다음 파티 만들기` · `내 증표함` · `50명까지 무료 / 51명부터 파티당 9,900원`

### 4.3 상태 처리 (A4)
- 로딩: daisyUI `loading-spinner`. 에러: `alert alert-error` + 사람이 읽을 문장(`불러오지 못했어요. 새로고침해 주세요`). 빈 상태: 3절 표의 문구.
- 더미 텍스트·Lorem·"테스트" 금지. 시드 데이터 없음(빈 상태 카피로 대응).
- 조건부 렌더는 삼항(`&&` 금지). 로딩/에러/성공은 `isPending ? … : isError ? … : …`.

### 4.4 접근성·에이전트 친화 (A1)
- 클릭 요소는 `<button>`/`<a href>`만. `<div onClick>` 금지. 폼은 `<form onSubmit>` + `<label htmlFor>`.
- 주요 요소에 `data-testid`: `create-party`, `party-name`, `host-name`, `party-size`, `invite-link`, `copy-invite`, `join-name`, `join-submit`, `meet-now`, `met-count`, `badge-first`, `tag-code`, `tag-submit`, `end-party`, `pick-submit`, `mutual-list`.
- 완료 텍스트는 DOM에 **남아 있는** 요소로(사라지는 토스트만으로 판정하지 않게). 토스트는 보조.
- URL 변화: 생성 후 `/party/<id>`.

### 4.5 반응형·안정성 (A2)
- `max-w-md mx-auto p-4`, 1열. 375×812 · 1280×720에서 가로 스크롤 0.
- 콘솔 에러·경고 0 (React key, act, 404 리소스 포함). `index.html`: `lang="ko"`, `<title>파티 패스포트</title>`, favicon 존재.
- 외부 의존: Firebase만. 실패 시 4.1 안내.

## 5. 도메인 로직 (순수 함수, `src/lib/party.ts`)

| 함수 | 규칙 |
|---|---|
| `newId(len)` | 혼동 문자 제외 31자 집합(`ABCDEFGHJKMNPQRSTUVWXYZ23456789`). pid 8자, code 4자 |
| `pairId(x, y)` | 사전순 정렬 `a_b` |
| `metIds(me, meets)` | 내가 포함된 만남의 상대 pid 목록 |
| `missionPartner(me, participants)` | `joinedAt` 오름차순(동률은 pid)으로 정렬 → 인덱스 i의 짝 = `i ^ 1`(0↔1, 2↔3 …). 짝 인덱스가 범위 밖이면 `null` |
| `badgesFor({ met, others, missionDone, mutual })` | 1 / 3 / `ceil(others/2)` / `others`(others>0) / missionDone / mutual≥1 |
| `isEnded({ createdAt, endedAt }, now)` | `endedAt != null || now − createdAt > 24h` |
| `priceFor(size)` | `size > 50 ? 9900 : 0` |

테스트: `src/lib/party.test.ts`, `node --test`(Node 24 타입 스트리핑, 의존성 0). `npm test`.

## 6. 데이터 접근

- `src/lib/firebase.ts`: app · auth · db. 웹 config는 공개값 → 커밋.
- `src/lib/useLive.ts`: `useLiveDoc<T>(path)`, `useLiveCollection<T>(path)` — `onSnapshot` → `{ data, error, isPending }`. 언마운트 시 해제. AGENTS.md의 `useQuery` 규칙은 REST 전제라 구독은 이 훅으로 대체(팀 문서 v2 결정, 비용 근거는 thinking 9절).
- 쓰기: `src/api/party.ts` 함수들을 `useMutation`으로 호출. 함수 계약은 backend PRD 5절.
- 결과 확인(`/party/:id` 종료 상태): `getDoc(picks/{pairId(me, other)})`를 **내가 고른 사람에 대해서만**, 진입 시 + 30초 간격 + `다시 확인` 버튼. 거부(permission-denied) = 아직 상호 아님으로 처리, 에러로 표시하지 않는다.

## 7. 파일 구조

```
src/
  main.tsx                 AuthGate로 감싼 라우터
  App.tsx                  Route 7개
  components/AuthGate.tsx  익명 인증 부팅
  pages/HomePage.tsx · NewPartyPage.tsx · PartyPage.tsx · JoinPage.tsx · MeetPage.tsx · PassportPage.tsx · NotFoundPage.tsx
  components/InviteBox.tsx · MyQr.tsx · Progress.tsx · TagByCode.tsx · Mission.tsx · Badges.tsx · ParticipantList.tsx · PickForm.tsx · MatchResult.tsx
  api/party.ts             Firestore 쓰기/읽기 함수
  lib/firebase.ts · useLive.ts · party.ts · party.test.ts · passport.ts(localStorage: me/host/passport)
```
삭제: `pages/LoginPage.tsx`, `components/RequireAuth.tsx`, `stores/useAuthStore.ts`. 추가 의존성: `firebase`, `qrcode.react`.

## 8. 수용 기준

1. **3단계 그대로**(thinking 10절 문장)를 Playwright(MCP, 심사와 같은 도구)로 배포 URL에서 **같은 탭**으로 완주 — 1280×720, 375×812 각 1회. 각 단계 완료 텍스트가 DOM에 존재, `browser_console_messages` 에러 0.
2. **새 브라우저 컨텍스트**에서 초대 링크 → 참여 → `지금 만났어요` → 양쪽 화면 `만난 사람` +1, 참가자 목록 양쪽 이름.
3. `코드로 태그`: 없는 코드 → `그 코드는 없어요`; 내 코드 → `내 코드예요…`; 정상 → 만남 +1; 같은 사람 재시도 → `이미 만났어요`.
4. 증표: 1명 → `첫 만남`, 짝과 만남 → `미션 완료`, `/passport`에 누적, 새로고침 후 유지.
5. 호스트 `파티 종료` → 양쪽 선택 화면 → 서로 고르면 `서로 선택된 사람`에 표시, 한쪽만 고르면 빈 상태 문구. 비호스트에게 `파티 종료` 없음.
6. 없는 파티 ID, 종료된 파티의 meet 링크, 익명 인증 실패 → 각 안내 화면.
7. `npm test` · `npm run build` · `npm run lint` 통과. 로그인 페이지 없음(`/login` → 404 안내).
