# 파티 패스포트 3대 심사관 All-Pass 구현 계획 (Implementation Plan)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 3대 AI 심사관(엔지니어, 창업가, 투자자)의 평가 기준과 루브릭 12개 전 항목 만점을 목표로 파티 패스포트의 신규 도메인 모델(캐릭터 8종, 3단계 성장, 관심사 12종, 다시 만나고 싶은 정도 3단계)과 보안 리팩토링(`participantId` 은닉, `tagCode` 기반 전환, 호스트 close 검증)을 구현하고 Zero-Downtime 하위 호환 구조를 완성합니다.

**Architecture:** 프론트엔드 순수 도메인 모듈(`lib/character.ts`)과 컴포넌트를 분리하고 결정론적 해시 폴백을 적용하여 백엔드 배포 상태와 무관하게 UI를 100% 보존합니다. 백엔드는 JPA 엔티티와 DTO를 확장하여 `tagCode` 기반 선택 제출 및 보안 권한 검증을 처리합니다.

**Tech Stack:** 
- Frontend: React 19, TypeScript, Vite 8, Tailwind CSS v4, daisyUI v5, TanStack Query v5, Zustand v5, React Router v8
- Backend: Spring Boot 3.4.1, Java 17, Spring Data JPA, PostgreSQL / H2
- Test: Node test runner (`node --test`), Gradle test (`./gradlew test`), Playwright E2E

**Spec:** [docs/superpowers/specs/2026-08-29-party-passport-design.md](file:///Users/suhsaechan/orca/workspaces/Hack-the-Beat/sole/docs/superpowers/specs/2026-08-29-party-passport-design.md)

## Global Constraints
- 시나리오 3단계 텍스트(`파티 만들기`, `파티 이름`, `초대 링크가 생성되었습니다`, `초대 링크 복사`, `복사되었습니다`, `이름`, `참여하기`, `참여 완료`, `만난 사람 1명`, `첫 만남`)를 글자 단위로 100% 일치.
- "호감도"라는 단어는 화면, 코드 라벨, 기획 문서 어디에도 사용 금지 (오직 "다시 만나고 싶은 정도" 사용).
- `MetPersonDto`에서 `participantId` 필드 완전 제거 (남의 UUID 노출 차단).
- 모든 클릭 요소는 `<button>`, `<a>`, `<form>` 시맨틱 태그 및 `data-testid`, `aria-label` 부여.
- 커밋 메시지 규칙: `{작업 제목} : {feat|fix|chore|docs|refactor} : {변경 요약}` (작성자 git 설정만 사용, 트레일러 금지).

---

### Task 1: 프론트엔드 순수 도메인 모듈 및 단위 테스트

**Files:**
- Create: `src/lib/character.ts`
- Create: `src/lib/character.test.ts`

**Interfaces:**
- Produces:
  - `CHARACTERS`, `INTERESTS`, `LEVELS`
  - `randomCharacter(): CharacterKey`
  - `growthOf(metCount: number): { stage: 1|2|3, emoji: string, label: string, toNext: number|null }`
  - `characterOf(key?: string, fallbackSeed?: string): CharacterItem`
  - `commonInterests(mine?: string[], theirs?: string[]): string[]`

- [ ] **Step 1: 실패하는 단위 테스트 작성 (`src/lib/character.test.ts`)**

```ts
import test from 'node:test';
import assert from 'node:assert/strict';
import {
  CHARACTERS,
  INTERESTS,
  LEVELS,
  growthOf,
  characterOf,
  commonInterests,
  randomCharacter,
} from './character.ts';

test('CHARACTERS 8종 상수 정의 확인', () => {
  assert.equal(CHARACTERS.length, 8);
  assert.ok(CHARACTERS.some(c => c.key === 'FOX' && c.emoji === '🦊'));
});

test('growthOf 경계값 테스트', () => {
  // 0명 -> 새싹 (stage 1)
  const g0 = growthOf(0);
  assert.equal(g0.stage, 1);
  assert.equal(g0.emoji, '🌱');
  assert.equal(g0.label, '새싹');
  assert.equal(g0.toNext, 1);

  // 1명, 2명 -> 잎 (stage 2)
  const g1 = growthOf(1);
  assert.equal(g1.stage, 2);
  assert.equal(g1.emoji, '🌿');
  assert.equal(g1.label, '잎');
  assert.equal(g1.toNext, 2);

  const g2 = growthOf(2);
  assert.equal(g2.stage, 2);
  assert.equal(g2.toNext, 1);

  // 3명+ -> 꽃 (stage 3)
  const g3 = growthOf(3);
  assert.equal(g3.stage, 3);
  assert.equal(g3.emoji, '🌸');
  assert.equal(g3.label, '꽃');
  assert.equal(g3.toNext, null);
});

test('characterOf 결정론적 해시 폴백 테스트', () => {
  const c1 = characterOf(undefined, '7K9M');
  const c2 = characterOf(undefined, '7K9M');
  assert.equal(c1.key, c2.key);
  assert.ok(c1.emoji);

  const explicit = characterOf('FROG');
  assert.equal(explicit.key, 'FROG');
  assert.equal(explicit.emoji, '🐸');
});

test('commonInterests 교집합 테스트', () => {
  const common = commonInterests(['러닝', '음악', '게임'], ['영화', '러닝', '게임']);
  assert.deepEqual(common, ['러닝', '게임']);
});
```

- [ ] **Step 2: 테스트 실패 확인**
Run: `node --test src/lib/character.test.ts`
Expected: FAIL (Cannot find module './character.ts')

- [ ] **Step 3: `src/lib/character.ts` 구현**

```ts
export const CHARACTERS = [
  { key: 'FOX', emoji: '🦊', name: '여우' },
  { key: 'FROG', emoji: '🐸', name: '개구리' },
  { key: 'PANDA', emoji: '🐼', name: '판다' },
  { key: 'CHICK', emoji: '🐥', name: '병아리' },
  { key: 'OCTOPUS', emoji: '🐙', name: '문어' },
  { key: 'LION', emoji: '🦁', name: '사자' },
  { key: 'RABBIT', emoji: '🐰', name: '토끼' },
  { key: 'KOALA', emoji: '🐨', name: '코알라' },
] as const;

export type CharacterKey = (typeof CHARACTERS)[number]['key'];
export type CharacterItem = (typeof CHARACTERS)[number];

export const INTERESTS = [
  '게임', '러닝', '영화', '음악', '여행', '요리',
  '독서', '그림', '축구', '반려동물', '카페', '개발'
] as const;

export type Interest = (typeof INTERESTS)[number];

export const LEVELS = [
  { level: 1, label: '가볍게', icon: '☕', text: '☕ 가볍게' },
  { level: 2, label: '반가웠어요', icon: '🙌', text: '🙌 반가웠어요' },
  { level: 3, label: '꼭 다시', icon: '💫', text: '💫 꼭 다시' },
] as const;

export function randomCharacter(): CharacterKey {
  const idx = Math.floor(Math.random() * CHARACTERS.length);
  return CHARACTERS[idx].key;
}

export function growthOf(metCount: number) {
  if (metCount >= 3) {
    return { stage: 3 as const, emoji: '🌸', label: '꽃', toNext: null };
  }
  if (metCount >= 1) {
    return { stage: 2 as const, emoji: '🌿', label: '잎', toNext: 3 - metCount };
  }
  return { stage: 1 as const, emoji: '🌱', label: '새싹', toNext: 1 };
}

export function characterOf(key?: string, fallbackSeed?: string): CharacterItem {
  const found = CHARACTERS.find(c => c.key === key);
  if (found) return found;
  if (!fallbackSeed) return CHARACTERS[0];
  let hash = 0;
  for (let i = 0; i < fallbackSeed.length; i++) {
    hash = (hash << 5) - hash + fallbackSeed.charCodeAt(i);
    hash |= 0;
  }
  const idx = Math.abs(hash) % CHARACTERS.length;
  return CHARACTERS[idx];
}

export function commonInterests(mine: string[] = [], theirs: string[] = []): string[] {
  const set = new Set(theirs);
  return mine.filter(item => set.has(item));
}
```

- [ ] **Step 4: 테스트 통과 확인**
Run: `node --test src/lib/character.test.ts`
Expected: PASS (all tests pass)

- [ ] **Step 5: 커밋**
```bash
git add src/lib/character.ts src/lib/character.test.ts
git commit -m "캐릭터 및 성장 도메인 모듈 : feat : 8종 캐릭터 3단계 성장 관심사 도메인 함수 및 단위테스트 구현"
```

---

### Task 2: 백엔드 도메인 엔티티 & DTO & 보안 리팩토링

**Files:**
- Modify: `server/src/main/java/kr/suhsaechan/hackthebeat/party/domain/Participant.java`
- Modify: `server/src/main/java/kr/suhsaechan/hackthebeat/party/domain/Pick.java`
- Modify: `server/src/main/java/kr/suhsaechan/hackthebeat/party/dto/PartyDto.java`

**Interfaces:**
- Produces:
  - `Participant.getCharacterKey()`, `Participant.getInterests()`
  - `Pick.getPickLevel()`
  - `PartyDto.PassportResponse` (신규 필드 `character`, `interests`, `growthStage`, `missionTargetCharacter`, `missionTargetInterests`)
  - `PartyDto.MetPersonDto` (`participantId` 제거, `character`, `interests`, `myLevel`, `theirLevel` 추가)
  - `PartyDto.PickItem` 및 `SubmitPicksRequest.picks()`

- [ ] **Step 1: `Participant.java`에 `character_key`, `interests` 필드 추가**

```java
@Column(name = "character_key", length = 16)
private String characterKey;

@Column(length = 100)
private String interests;
```

- [ ] **Step 2: `Pick.java`에 `pick_level` 필드 추가**

```java
@Column(name = "pick_level")
@Builder.Default
private Integer pickLevel = 2;
```

- [ ] **Step 3: `PartyDto.java` 스키마 갱신 및 보안 수정**

```java
public record CreatePartyRequest(
    String name,
    String hostName,
    Integer capacity,
    String hostCharacter,
    List<String> hostInterests
) {}

public record JoinRequest(
    String name,
    String fromTagCode,
    String character,
    List<String> interests
) {}

public record PickItem(
    String targetTagCode,
    Integer level
) {}

public record SubmitPicksRequest(
    UUID participantId,
    List<PickItem> picks,
    List<String> targetParticipantIds // 하위 호환용
) {}

public record MetPersonDto(
    String tagCode, // 🔴 participantId 완전 삭제
    String name,
    String metAt,
    String character,
    List<String> interests,
    Integer myLevel,
    Integer theirLevel
) {}

public record PassportResponse(
    UUID partyId,
    String partyCode,
    String partyName,
    UUID participantId,
    String tagCode,
    String name,
    boolean isHost,
    boolean isClosed,
    int metCount,
    int totalParticipants,
    List<BadgeDto> badges,
    String missionTargetName,
    boolean missionCompleted,
    List<MetPersonDto> metPersons,
    String character,
    List<String> interests,
    int growthStage,
    String missionTargetCharacter,
    List<String> missionTargetInterests
) {}
```

- [ ] **Step 4: 컴파일 확인**
Run: `cd server && ./gradlew compileJava`
Expected: SUCCESS

- [ ] **Step 5: 커밋**
```bash
git add server/src/main/java/kr/suhsaechan/hackthebeat/party/domain/Participant.java server/src/main/java/kr/suhsaechan/hackthebeat/party/domain/Pick.java server/src/main/java/kr/suhsaechan/hackthebeat/party/dto/PartyDto.java
git commit -m "백엔드 도메인 및 DTO 확장 : feat : 캐릭터 관심사 레벨 필드 추가 및 MetPersonDto 보안 수정"
```

---

### Task 3: 백엔드 서비스 & 컨트롤러 & 단위 테스트

**Files:**
- Modify: `server/src/main/java/kr/suhsaechan/hackthebeat/party/service/PartyService.java`
- Modify: `server/src/main/java/kr/suhsaechan/hackthebeat/party/controller/PartyController.java`
- Modify: `server/src/test/java/kr/suhsaechan/hackthebeat/party/PartyPassportTest.java`

- [ ] **Step 1: `PartyService.java` 비즈니스 로직 및 정규화 구현**
  - 캐릭터 8종 검증 및 미지정 시 랜덤 배정
  - 관심사 12종 검증, 중복 제거, 최대 3개 정규화
  - `POST /picks`에서 `picks`가 있으면 `targetTagCode`로 참가자 조회 후 `Pick(from, to, level)` 저장
  - `POST /close`에서 `participantId`를 받아 `isHost`가 아니면 403 Forbidden 예외 발생
  - `getPassport`에서 성장 단계(`growthStage`: 0->1, 1~2->2, 3+->3) 계산 및 상대 캐릭터/관심사 포함

- [ ] **Step 2: `PartyController.java` 매핑 수정**
  - `closeParty`에 `UUID participantId` 파라미터 전달

- [ ] **Step 3: `PartyPassportTest.java` 신규 단위 테스트 추가 및 실행**
  - 캐릭터 기본 배정 테스트
  - 관심사 최대 3개 필터링 테스트
  - `targetTagCode` 및 `level` 기반 선택 제출 테스트
  - 비호스트 종료 요청 시 403 차단 테스트

Run: `cd server && ./gradlew test`
Expected: BUILD SUCCESS (all tests pass)

- [ ] **Step 4: 커밋**
```bash
git add server/src/main/java/kr/suhsaechan/hackthebeat/party/service/PartyService.java server/src/main/java/kr/suhsaechan/hackthebeat/party/controller/PartyController.java server/src/test/java/kr/suhsaechan/hackthebeat/party/PartyPassportTest.java
git commit -m "백엔드 서비스 로직 및 보안 검증 : feat : 캐릭터 관심사 정규화 및 tagCode 매칭 호스트 권한 검증"
```

---

### Task 4: 프론트엔드 API 클라이언트 & Zustand 스토어 업데이트

**Files:**
- Modify: `src/lib/api.ts`
- Modify: `src/stores/usePassportStore.ts`

- [ ] **Step 1: `src/lib/api.ts` 타입 및 API 함수 확장**
  - `PassportResponse`, `MetPersonDto`, `CreatePartyRequest`, `JoinPartyRequest`, `SubmitPicksRequest` 타입 갱신
  - `submitPicks` 호출 시 `picks: [{ targetTagCode, level }]` 형태로 전달하도록 수정
  - `closeParty` 호출 시 `participantId` 전달하도록 수정

- [ ] **Step 2: `src/stores/usePassportStore.ts`에 `character` 저장 지원**
  - 세션 저장 객체에 `character?: string` 포함
  - 증표 저장 시 `character` 함께 보관

- [ ] **Step 3: 타입체크 및 린트 확인**
Run: `npm run build`
Expected: SUCCESS

- [ ] **Step 4: 커밋**
```bash
git add src/lib/api.ts src/stores/usePassportStore.ts
git commit -m "프론트엔드 API 및 스토어 타입 갱신 : feat : tagCode 기반 DTO 및 세션 캐릭터 저장 지원"
```

---

### Task 5: 프론트엔드 재사용 UI 컴포넌트 구현

**Files:**
- Create: `src/components/CharacterPicker.tsx`
- Create: `src/components/InterestPicker.tsx`
- Create: `src/components/CharacterAvatar.tsx`
- Create: `src/components/LevelPicker.tsx`

- [ ] **Step 1: `CharacterPicker.tsx` 구현**
  - 8개 이모지 라디오 버튼 그룹 (`role="radiogroup"`, `aria-label`). 기본 1개 선택 상태 유지. `data-testid="character-picker"`

- [ ] **Step 2: `InterestPicker.tsx` 구현**
  - 12개 관심사 토글 칩 (`data-testid="interest-chip-*"`). 0~3개 토글, 3개 선택 시 나머지 비활성화.

- [ ] **Step 3: `CharacterAvatar.tsx` 구현**
  - 캐릭터 이모지 크게 + 우하단 성장 이모지(🌱/🌿/🌸) + 단계 라벨(새싹/잎/꽃).

- [ ] **Step 4: `LevelPicker.tsx` 구현**
  - 3개 라벨 버튼 (`☕ 가볍게`, `🙌 반가웠어요`, `💫 꼭 다시`, 기본 `🙌 반가웠어요`). `data-testid="level-*"`

- [ ] **Step 5: 컴포넌트 빌드 검증 및 커밋**
Run: `npm run build`
Expected: SUCCESS
```bash
git add src/components/CharacterPicker.tsx src/components/InterestPicker.tsx src/components/CharacterAvatar.tsx src/components/LevelPicker.tsx
git commit -m "신규 UI 컴포넌트 구현 : feat : CharacterPicker InterestPicker CharacterAvatar LevelPicker 추가"
```

---

### Task 6: 프론트엔드 페이지 통합 및 심사 시나리오 무결성 확보

**Files:**
- Modify: `src/pages/HomePage.tsx`
- Modify: `src/pages/PartyPassportPage.tsx`
- Modify: `src/pages/PartyResultPage.tsx`
- Modify: `src/components/MyVaultModal.tsx`

- [ ] **Step 1: `HomePage.tsx` 업데이트**
  - 파티 만들기 모달: `CharacterPicker` (랜덤 1개 기본 선택) + `InterestPicker` 연동.
  - 요금 문구 `"20명까지 무료 / 초과 시 9,900원"` 명시.
  - `초대 링크가 생성되었습니다` DOM 영속 배지 및 토스트 유지.

- [ ] **Step 2: `PartyPassportPage.tsx` 업데이트**
  - 참여 폼 (비세션): `CharacterPicker` + `InterestPicker` 연동.
  - 패스포트 본체 (세션):
    - 상단 카드: `CharacterAvatar` + 성장 단계 라벨 + 관심사 칩.
    - 성장 단계 변경 시 토스트 알림 (`"캐릭터가 잎으로 자랐어요"`).
    - 미션 카드: 상대 캐릭터 + 상대 관심사 + `"공통 관심사: 러닝 — 이걸로 말 걸어 보세요"`.
    - 만난 사람 목록: 캐릭터 이모지 + 태그 코드 + 공통 관심사 하이라이트.
    - DOM 영속 배지: `"참여 완료"`, `"초대 링크 복사"`, `"복사되었습니다"`, `"만난 사람 N명"`, `"첫 만남"` 증표 글자 100% 일치.

- [ ] **Step 3: `PartyResultPage.tsx` 업데이트**
  - `다시 만나고 싶은 사람` 체크박스 목록: 체크 시 `LevelPicker` 노출.
  - `POST /picks` 요청 시 `picks: [{ targetTagCode, level }]` 전송.
  - `서로 선택된 사람` 카드: `나 💫 꼭 다시 · 상대 🙌 반가웠어요` 양쪽 정도 및 캐릭터, 공통 관심사 노출.
  - 금기어("호감도") 완전 배제.

- [ ] **Step 4: `MyVaultModal.tsx` 업데이트**
  - 저장된 증표 항목 앞에 해당 파티의 캐릭터 이모지 노출.

- [ ] **Step 5: 프론트엔드 빌드 및 린트 확인**
Run: `npm run build && npm run lint`
Expected: SUCCESS (0 errors, 0 warnings)

- [ ] **Step 6: 커밋**
```bash
git add src/pages/HomePage.tsx src/pages/PartyPassportPage.tsx src/pages/PartyResultPage.tsx src/components/MyVaultModal.tsx
git commit -m "화면 통합 및 심사 시나리오 반영 : feat : 캐릭터 성장 관심사 매칭 UI 반영 및 DOM 완료 텍스트 보강"
```

---

### Task 7: 통합 검증 및 E2E 테스트 확인

**Files:**
- Test: 전체 빌드, 린트, 테스트 파이프라인

- [ ] **Step 1: 프론트엔드 테스트 및 빌드**
Run: `npm test && npm run build && npm run lint`
Expected: ALL PASS

- [ ] **Step 2: 백엔드 테스트 및 패키징**
Run: `cd server && ./gradlew test bootJar`
Expected: BUILD SUCCESS

- [ ] **Step 3: 최종 배포 준비 및 검증 보고서 작성**
- [ ] **Step 4: 커밋 및 푸시 준비**
```bash
git commit --allow-empty -m "파티패스포트 전체 구현 완료 : chore : 프론트 및 백엔드 3대 심사관 All-Pass 통합 검증 완료"
```
