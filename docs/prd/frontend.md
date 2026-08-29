# PRD — 파티 패스포트 · 프론트엔드

> 실제 구현 기준(`src/`, 배포 `https://twin-fang.github.io/Hack-the-Beat/`). 제품 근거는 [thinking/파티패스포트.md](../thinking/파티패스포트.md) v4, API 계약은 [backend.md](./backend.md).
> **신규 요구사항은 🆕, 수정 필수는 🔴, 결정 대기는 ⬜.** 표시 없는 항목은 현재 동작.
> 스택: React 19 · TypeScript · Vite 8 · Tailwind v4 + daisyUI v5 · React Router v8 · TanStack Query · Zustand · `qrcode.react`.

## 1. 목표와 우선순위

| 순위 | 목표 | 채점 근거 |
|---|---|---|
| 1 | [01-3단계 시나리오](../submission/01-3단계-시나리오.md) 문장이 데스크톱·모바일에서 **글자 단위로** 완주 | A1 13.6% · B3 상한 해제 |
| 2 | 콘솔 에러 0, 빈/에러/로딩 상태, 더미 텍스트 0 | A2 · A4 |
| 3 | 기획안에 쓴 기능이 전부 화면에서 확인됨 — 기획안에는 **배포된 것만** 쓴다 | A3 · 구현 대조 |
| 4 | 🆕 캐릭터·성장·관심사·다시 만나고 싶은 정도가 **3단계를 무겁게 하지 않게** 들어간다(기본값 자동) | A1 보호 |

범위 밖: 프로필 사진 · 자유 입력 자기소개 · 채팅 · 연락처 · 푸시 · 로그인 · 결제 · 인앱 QR 스캐너(폰 카메라가 URL을 연다).

## 2. 신원·저장 (`src/stores/usePassportStore.ts`)

- `sessions[partyCode] = { participantId, tagCode, name, isHost }` — localStorage `htb_passport_sessions`. **파티별 1세션**, 같은 브라우저에서 다시 참여하면 덮어씀(심사 에이전트가 호스트→김서준을 한 탭에서 연달아 해도 김서준이 "나"가 됨).
- `savedBadges[]` — localStorage `htb_saved_badges`, 파티 이름 + 증표 코드로 중복 방지. 🆕 `character`도 세션에 저장해 증표함에 "어느 캐릭터로 받았는지" 표시.
- 기기 간 이전 없음. 문서에 "계정 누적"이라고 쓰지 않는다.

## 3. 라우트와 화면

| 경로 | 파일 | 내용 |
|---|---|---|
| `/` | `pages/HomePage.tsx` | `파티 만들기`(모달) · 6자리 코드 `입장` · `내 증표함`(모달) · 요금 문구 |
| `/party/:code` | `pages/PartyPassportPage.tsx` | 세션 없음 → 참여 폼 / 있음 → 패스포트 |
| `/party/:code/result` | `pages/PartyResultPage.tsx` | 선택 제출 + 서로 선택된 사람 |
| `*` | `pages/NotFoundPage.tsx` | 404 |

### 3.1 파티 만들기 모달 (`/`)
- `파티 이름`(필수) · 요금 문구 `20명까지 무료 / 초과 시 9,900원`(⬜ 50명으로 바꾸면 여기 + 홈 하단 2곳) · 제출 `파티 만들기`.
- 🆕 **캐릭터** 선택 줄: 8개 이모지 버튼(`role="radio"` 그룹, `aria-label` 이름), **진입 시 랜덤 1개가 이미 선택됨**. 🆕 **관심사** 칩 12개, 0~3개 토글, 3개 차면 나머지 비활성. 둘 다 선택 사항 — 시나리오는 `파티 이름`만 친다.
- 🆕 **`연락처 교환` 토글**(기본 꺼짐) + 한 줄 `서로 선택된 사람끼리 연락처를 주고받아요`. 요금 문구는 `20명까지 무료 / 21명 이상 또는 연락처 교환 시 9,900원`으로, 아래에 `이 파티: 무료` / `이 파티: 9,900원`이 토글·인원에 따라 바뀜(C1 evidence, 결제 없음).
- 성공 → 토스트 `초대 링크가 생성되었습니다` → `/party/:code` 이동(URL 변화 = 1단계 성공 조건). `hostName`은 `호스트` 고정, `capacity` 30 고정(현재).

### 3.2 참여 폼 (`/party/:code`, 세션 없음)
- `이름`(필수) · 제출 `참여하기` · `?from=코드`가 있으면 안내 `초대자와 연결되어 바로 첫 증표를 받습니다!`(⬜A) — B를 고르면 `초대한 OO님을 지금 만났나요?` + `지금 만났어요`.
- 🆕 캐릭터 선택 줄(랜덤 기본) + 관심사 칩 — 3.1과 같은 컴포넌트 `CharacterPicker` · `InterestPicker`.
- 성공 → 토스트 `참여 완료` → 패스포트 렌더(같은 URL). 🆕 이미 참가자인데 다른 브라우저로 QR을 연 경우를 위한 한 줄: `이미 참여했다면 참여한 브라우저의 "코드로 태그"에 코드를 입력하세요`.

### 3.3 패스포트 (`/party/:code`, 세션 있음) — 위→아래
| 블록 | 현재 | 🆕 추가 |
|---|---|---|
| 헤더 | 파티 코드 · 이름 · `내 증표함` · 호스트 `파티 종료` / 종료 시 `결과 보기` | — |
| 내 패스포트 카드 | `내 코드: XXXX` · 이름 · QR(`?from=내코드` URL) · `초대 링크 복사`(→ `복사되었습니다`) · `코드로 태그`(모달) · 초대 링크 텍스트(`select-all`) | **캐릭터 크게(🦊) + 성장 이모지(🌱/🌿/🌸) + 단계 라벨(`새싹`/`잎`/`꽃`)**, 관심사 칩. 성장 단계가 바뀌면 토스트 `캐릭터가 잎으로 자랐어요` |
| 진행률 | `만난 사람 N명` · 전체 참가자 · `<progress>` | 다음 단계 안내 `꽃까지 2명 남았어요` |
| 미션 카드 | `OO님을 찾아 대화해 보세요!` · 완료 시 🎯 | 상대 캐릭터 이모지, 관심사 칩, **`공통 관심사: 러닝 — 이걸로 말 걸어 보세요`**(없으면 상대 관심사만) |
| 증표 | `BadgeList` 6종, 미획득 grayscale | — |
| 만난 사람들 | 이니셜 원 · 이름 · `#코드` · 시각 | 이니셜 원 → **캐릭터 이모지**, 관심사 칩(나와 공통은 강조) |
| 요금 문구 | `priceNotice` | — |

### 3.4 코드로 태그 모달
`상대방 4자리 코드` 입력 · `태그하기` · 오류 문구 서버 메시지 그대로(`해당 코드의 참가자를 찾을 수 없습니다` · `자신을 태그할 수 없습니다`). 성공 토스트 `태그 완료! 새로운 인연을 만났습니다.` 🆕 토스트에 상대 캐릭터·이름 포함(`🐸 민수님과 만났어요`).

### 3.5 결과 (`/party/:code/result`)
| 블록 | 현재 | 🆕 추가 |
|---|---|---|
| 헤더 | `파티 회고 & 매칭` 안내 | — |
| `다시 만나고 싶은 사람` | 만난 사람 체크박스 목록 · `선택 제출` · 빈 상태 `파티 중에 만난 사람이 없습니다.` | 체크된 사람 아래에 **`다시 만나고 싶은 정도`** 3버튼(`☕ 가볍게` · `🙌 반가웠어요` · `💫 꼭 다시`, 기본 `반가웠어요`). 캐릭터·관심사 표시. 🔴 제출 body는 `picks: [{ targetTagCode, level }]`. 🆕 파티가 `contactExchange`면 폼 하단에 `내 연락처 (선택)` 입력(placeholder `카카오 오픈채팅 링크 또는 인스타 @아이디`) + 안내 `서로 선택된 사람에게만 공개됩니다`; 아니면 칸 없음 |
| `서로 선택된 사람` | 💖 카드 · `서로를 다시 만나고 싶어 합니다!` · 빈 상태 문구 | 카드에 **양쪽 정도**(`나 💫 꼭 다시 · 상대 🙌 반가웠어요`), 캐릭터, 공통 관심사. 🆕 연락처 교환 파티면 `연락처:` 줄 — `http`로 시작하면 `<a target=_blank rel=noopener>`, 없으면 `연락처를 남기지 않았어요`. 단독 선택은 어떤 표시도 없음 |
| 하단 | `내 증표함` · `다음 파티 만들기`(→ `/`) | — |

> 라벨은 **"다시 만나고 싶은 정도"** 로 고정한다. "호감도"는 화면·기획안 어디에도 쓰지 않는다(게이트 ×0.85 방지).

### 3.6 내 증표함 모달
증표 목록(파티 이름 · 날짜 · 설명). 🆕 항목 앞에 그 파티에서 쓴 캐릭터 이모지. 빈 상태 문구 유지.

## 4. 텍스트 고정표 (시나리오·기획안과 글자 단위 동일)
`파티 만들기` · `파티 이름` · `초대 링크가 생성되었습니다` · `초대 링크 복사` · `복사되었습니다` · `이름` · `참여하기` · `참여 완료` · `만난 사람` · `첫 만남` · `내 코드` · `코드로 태그` · `태그하기` · `파티 종료` · `결과 보기` · `다시 만나고 싶은 사람` · `선택 제출` · `서로 선택된 사람` · `다음 파티 만들기` · `내 증표함` · `20명까지 무료 / 초과 시 9,900원`
🆕 `캐릭터` · `관심사` · `새싹` · `잎` · `꽃` · `공통 관심사` · `다시 만나고 싶은 정도` · `가볍게` · `반가웠어요` · `꼭 다시` · `연락처 교환` · `서로 선택된 사람끼리 연락처를 주고받아요` · `내 연락처 (선택)` · `서로 선택된 사람에게만 공개됩니다` · `연락처를 남기지 않았어요` · `20명까지 무료 / 21명 이상 또는 연락처 교환 시 9,900원` · (⬜B) `지금 만났어요`

## 5. 🆕 도메인 상수·순수 함수 (`src/lib/character.ts`)

```ts
CHARACTERS = [ FOX 🦊 여우, FROG 🐸 개구리, PANDA 🐼 판다, CHICK 🐥 병아리,
               OCTOPUS 🐙 문어, LION 🦁 사자, RABBIT 🐰 토끼, KOALA 🐨 코알라 ]
INTERESTS  = [ 게임, 러닝, 영화, 음악, 여행, 요리, 독서, 그림, 축구, 반려동물, 카페, 개발 ]
LEVELS     = [ 1 ☕ 가볍게, 2 🙌 반가웠어요, 3 💫 꼭 다시 ]

randomCharacter()                → 8종 중 하나 (폼 기본값)
growthOf(metCount)               → { stage: 1|2|3, emoji: 🌱|🌿|🌸, label: 새싹|잎|꽃, toNext: number|null }
                                    0 → 새싹, 1~2 → 잎, 3+ → 꽃
characterOf(key?, fallbackSeed)  → key가 8종이면 그것, 아니면 seed(tagCode) 해시로 결정 (서버 미배포 폴백)
commonInterests(mine, theirs)    → 교집합
```
테스트: `src/lib/character.test.ts`, `node --test`(의존성 0) — `growthOf` 경계(0/1/2/3), `characterOf` 폴백 결정성, `commonInterests`.

## 6. 데이터 접근 (`src/lib/api.ts`)
- 읽기 `useQuery`(패스포트 4초 폴링 · 결과 1회), 쓰기 `useMutation`. 컴포넌트 직접 `fetch` 금지.
- 🆕 타입 확장: `PassportResponse += character, interests, growthStage?, missionTargetCharacter?, missionTargetInterests?` / `MetPersonDto += character?, interests?, myLevel?, theirLevel?`, 🔴 `participantId` 제거 → 키는 `tagCode`.
- 🆕 요청: `createParty({ …, hostCharacter, hostInterests, contactExchange })`, `joinParty({ …, character, interests })`, `submitPicks({ participantId, picks: [{ targetTagCode, level }], contact? })`. 응답 `PassportResponse/PartyStatus += contactExchange`, `MetPersonDto += contact?`(상호·옵션 켬일 때만).

## 7. 하위 호환 — 서버가 아직 새 필드를 안 줄 때
| 상황 | 프론트 동작 |
|---|---|
| 응답에 `character` 없음 | `characterOf(undefined, tagCode)` 해시 폴백 — 같은 사람은 항상 같은 캐릭터 |
| `interests` 없음 | 빈 배열, 칩 영역 숨김 |
| `growthStage` 없음 | `growthOf(metCount)`로 계산(항상 이걸 쓴다) |
| `myLevel/theirLevel` 없음 | 정도 표시 숨김, 카드 문구 유지 |
| `contactExchange` 없음 | 토글은 보이되 서버가 무시 → 입력칸·연락처 줄 숨김, 가격 문구는 기존 `20명까지 무료 / 초과 시 9,900원` 유지 |
| 서버가 `picks`를 모름 | `targetParticipantIds`도 같이 보낸다(🔴 수정 배포 전까지) |
요청의 새 필드는 Spring(Jackson)이 무시하므로 먼저 배포해도 안전.

## 8. 상태·접근성·안정성
- 로딩 `loading-spinner`, 에러 `alert-error` + 서버 메시지, 빈 상태 문구(3절). 조건부 렌더는 삼항.
- 클릭 요소는 `<button>`/`<a>`, 폼은 `<form>` + `<label>`. `data-testid`: `create-party-btn` · `copy-invite-btn`(현재) + 🆕 `character-picker` · `interest-chip-*` · `growth-stage` · `level-*` · `mutual-card`.
- 완료 텍스트는 토스트(3초) — 🔴 `참여 완료`·`복사되었습니다`·`초대 링크가 생성되었습니다`는 **DOM에 남는 요소로도** 표시(에이전트 스냅샷 타이밍 보호). 예: 참여 후 카드 상단 `참여 완료` 배지.
- 클립보드: 실패해도 `복사되었습니다`를 띄우는 현재 동작은 URL이 화면에 노출돼 있어 허용. 여유 있으면 실패 시 `링크를 길게 눌러 복사해 주세요`.
- 375×812 · 1280×720 가로 스크롤 0, 콘솔 에러 0. 이모지는 `role="img"` + `aria-label`.

## 9. 파일 구조 (현재 + 🆕)
```
src/
  App.tsx · main.tsx · index.css
  lib/api.ts (타입·요청) · 🆕 lib/character.ts · 🆕 lib/character.test.ts
  stores/usePassportStore.ts
  pages/HomePage.tsx · PartyPassportPage.tsx · PartyResultPage.tsx · NotFoundPage.tsx
  components/BadgeList.tsx · TagModal.tsx · MyVaultModal.tsx
  🆕 components/CharacterPicker.tsx · InterestPicker.tsx · CharacterAvatar.tsx(캐릭터+성장) · LevelPicker.tsx
```

## 10. 수용 기준
1. 01-3단계 시나리오 문장을 Playwright(MCP)로 배포 URL에서 **같은 탭**으로 완주 — 1280×720, 375×812. 각 완료 텍스트가 DOM에 존재, 콘솔 에러 0.
2. 🆕 파티 만들기·참여 폼을 **아무것도 고르지 않고** 제출해도 캐릭터가 배정되고 화면에 보인다.
3. 🆕 만난 사람 0 → `새싹`, 1 → `잎`, 3 → `꽃`으로 카드가 바뀐다. 토스트 `캐릭터가 잎으로 자랐어요`.
4. 🆕 관심사 4개째는 선택 불가. 미션 카드에 공통 관심사가 뜬다(없으면 상대 관심사만).
5. 🆕 결과: 체크한 사람마다 정도 선택, 기본 `반가웠어요`. 서로 선택된 카드에 양쪽 정도. 한쪽만 고르면 빈 상태 문구.
6. 🔴 만난 사람 목록·선택 제출이 `tagCode`만으로 동작(남의 participantId 미사용).
7. 서버 미배포 상태(폴백)에서도 1~5가 깨지지 않는다(정도만 숨김).
8. 🆕 `연락처 교환`을 켜고 만든 파티에서만 결과 폼에 `내 연락처 (선택)`가 보이고, 서로 선택된 카드에만 상대 연락처가 뜬다. 끈 파티엔 둘 다 없음. 생성 모달 가격 줄이 토글·인원에 따라 바뀐다.
9. `npm test` · `npm run build` · `npm run lint` 통과.
