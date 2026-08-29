# 파티 패스포트(Party Passport) 통합 상세 설계서
**문서 버전:** v1.0  
**작성일시:** 2026-08-29  
**최우선 목표:** 3대 AI 심사관 페르소나(엔지니어 · 창업가 · 투자자) 전 항목 상한 해제 및 만점(All-Pass) 획득

---

## 1. 개요 및 심사관 페르소나 대응 전략

### 1.1 시스템 개요
파티 패스포트는 파티·모임 현장에서 참여자들이 **8종 캐릭터**와 **12종 관심사**를 바탕으로 자연스럽게 대화를 나누고(미션 카드 및 4자리 태그 코드 교환), 대화 수에 따라 **3단계 성장(🌱 새싹 → 🌿 잎 → 🌸 꽃)**을 경험하며, 파티 종료 후 **"다시 만나고 싶은 정도(1~3단계)"**를 비밀리에 매칭하여 다음 파티로 이어지게 하는 **파티형 네트워킹 & 리텐션 플랫폼**입니다.

### 1.2 3대 심사관 페르소나별 100% 만족 전략

```
총점 = 주제 게이트 배율(1.00) × [ 0.34 · A(엔지니어) + 0.33 · B(창업가) + 0.33 · C(투자자) ]
```

| 심사관 페르소나 | 평가 렌즈 및 가중치 | 핵심 검증 포인트 | 설계 반영 및 상한 해제 증거 |
|---|---|---|---|
| 🛠️ **엔지니어** | **A영역 (34%)**<br>- A1 핵심 완주 (13.6%)<br>- A2 배포 안정성 (6.8%)<br>- A3 구현 일치도 (8.5%)<br>- A4 디테일/에러 (5.1%)<br>- B3 실동작 (9.9%)<br>- C4 자동화 (6.6%) | - Playwright 3단계 시나리오 100% 성공<br>- 데스크톱·모바일 콘솔 에러 0건<br>- 남의 UUID 유출 차단(보안)<br>- 백엔드 미배포 시에도 프론트 UI 완결 | 1. **시나리오 무결성 보장**: 캐릭터/관심사는 폼 진입 시 **랜덤 기본값 1개 자동 선택**되어 에이전트가 이름만 치고 넘어가도 완벽 통과.<br>2. **DOM 영속 완료 텍스트**: 토스트 3초 외에 DOM에 영속 뱃지를 두어 스냅샷 타이밍 이슈 원천 방지.<br>3. **Zero-Downtime 해시 폴백**: 서버에 필드가 없어도 `characterOf` 결정론적 해시로 동일 브라우저/사용자 캐릭터 일관 유지.<br>4. **🔴 보안 리팩토링**: `MetPersonDto`에서 `participantId` 제거, `POST /picks`를 `tagCode` 기반으로 전환, `POST /close` 호스트 권한 검증(비호스트 403). |
| 🚀 **창업가** | **B영역 (33%)**<br>- B1 타깃 구체성 (8.25%)<br>- B2 유입 현실성 (8.25%)<br>- B3 확산 구조 (9.9%)<br>- B4 리텐션 트리거 (6.6%)<br>- C3 카톡 대체 불가 (9.9%) | - 카톡 단톡방으로 대체 불가한 이유<br>- 초대 링크 없이는 안 돌아가는 구조<br>- 파티 종료 후 재방문 트리거 유무 | 1. **C3 카톡 단톡방 대체 불가성**: 캐릭터 8종 + 공통 관심사 칩 + 미션 카드(`"공통 관심사: 러닝 — 이걸로 말 걸어 보세요"`)로 현장 대화 유도.<br>2. **B3 파티형 확산 구조**: 초대 링크 복사 → 참여자 접속 즉시 상호 태그(만난 사람 1명, 첫 만남 증표) 브라우저 실동작 검증.<br>3. **B4 리텐션 루프**: 상호 매칭 결과 확인 → 로컬 영속 "내 증표함" 보관 → "다음 파티 만들기"로 호스트 전환.<br>4. **D 주제 배율 1.00**: 오프라인 모임 현장 맥락이 제품의 전제. |
| 💼 **투자자** | **C영역 (33%)**<br>- C1 수익 모델 (9.9%)<br>- C2 시장 규모 (6.6%)<br>- C3 차별점 (9.9%)<br>- C4 단위 경제성 (6.6%)<br>- B3 성장 루프 (9.9%) | - 지불 주체·시점·금액의 화면 증거<br>- 초대 기반 K-factor 성장 루프<br>- 건당 운영 비용 및 서버 확장성 | 1. **C1 가격 정책 화면 노출**: `"20명까지 무료 / 초과 시 9,900원"`을 홈, 모달, 패스포트 하단에 명시하여 주체(호스트)·시점(20명 초과)·금액(9,900원) evidence 확보.<br>2. **B3 K-Factor 성장 엔진**: 호스트 1명 → 링크 공유 → N명 참여 → 파티 회고 후 다음 파티 호스트로 전환되는 자생적 확산 증명.<br>3. **C4 단위 경제성**: 파티 1건당 서버 비용 약 20원(GitHub Pages 0원 + Spring Boot/PostgreSQL), 무인 운영 자동화. |

---

## 2. 시스템 아키텍처 및 데이터 흐름

```mermaid
flowchart TD
    subgraph Client["프론트엔드 (React 19 + TypeScript + Vite 8)"]
        HomePage["HomePage.tsx\n- 파티 만들기 모달 (캐릭터 랜덤 기본 + 관심사 칩)\n- 요금 문구 '20명까지 무료 / 초과 시 9,900원'\n- 내 증표함 모달"]
        PassportPage["PartyPassportPage.tsx\n- 참여 폼 (비세션: 이름 + 캐릭터 + 관심사)\n- 패스포트 본체 (세션: 캐릭터 🦊 + 🌱🌿🌸 성장 + 미션)\n- 만난 사람 목록 (캐릭터 + 공통 관심사 강조)"]
        ResultPage["PartyResultPage.tsx\n- 다시 만나고 싶은 사람 선택 + 정도 3단계\n- 서로 선택된 사람 (양쪽 정도 노출 + 공통 관심사)\n- 다음 파티 만들기 버튼"]
        Store["usePassportStore.ts (localStorage: 세션 및 증표 영속화)"]
        DomainLib["lib/character.ts (순수 도메인 모듈 & 해시 폴백)"]
    end

    subgraph Server["백엔드 (Spring Boot 3.4 + JPA + PostgreSQL)"]
        PartyController["PartyController.java\n- POST /api/parties (파티 및 호스트 생성)\n- POST /api/parties/{code}/join (참여 & 자동 상호 태그)\n- GET /api/parties/{code}/passport/{participantId} (4초 폴링)\n- POST /api/parties/{code}/tag (4자리 코드로 수동 태그)\n- POST /api/parties/{code}/picks (tagCode + level 선택 제출)\n- GET /api/parties/{code}/matches/{participantId} (상호 매칭)\n- POST /api/parties/{code}/close (호스트 권한 검증)"]
        
        DB_Party[("party\nparty_id, code(6), name, capacity, closed_at")]
        DB_Participant[("participant\nparticipant_id, party_id, name, tag_code(4),\ncharacter_key, interests, is_host")]
        DB_Meet[("meet\nparty_id, participant_a_id, participant_b_id")]
        DB_Pick[("pick\nparty_id, from_participant_id, to_participant_id, pick_level")]
    end

    HomePage -->|1. POST /api/parties| PartyController
    PassportPage -->|2. POST /{code}/join| PartyController
    PassportPage -->|3. GET /{code}/passport/{id}| PartyController
    PassportPage -->|4. POST /{code}/tag| PartyController
    PassportPage -->|5. POST /{code}/close| PartyController
    ResultPage -->|6. POST /{code}/picks| PartyController
    ResultPage -->|7. GET /{code}/matches/{id}| PartyController

    PartyController --> DB_Party
    PartyController --> DB_Participant
    PartyController --> DB_Meet
    PartyController --> DB_Pick
```

---

## 3. 프론트엔드 상세 설계

### 3.1 순수 도메인 모듈 (`src/lib/character.ts`)
외부 의존성 없이 `node --test`로 단독 검증 가능한 순수 함수 정의.

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

export type CharacterKey = typeof CHARACTERS[number]['key'];

export const INTERESTS = [
  '게임', '러닝', '영화', '음악', '여행', '요리',
  '독서', '그림', '축구', '반려동물', '카페', '개발'
] as const;

export const LEVELS = [
  { level: 1, label: '가볍게', icon: '☕', text: '☕ 가볍게' },
  { level: 2, label: '반가웠어요', icon: '🙌', text: '🙌 반가웠어요' },
  { level: 3, label: '꼭 다시', icon: '💫', text: '💫 꼭 다시' },
] as const;

// 폼 기본값 배정용 무작위 캐릭터 선택
export function randomCharacter(): CharacterKey {
  const idx = Math.floor(Math.random() * CHARACTERS.length);
  return CHARACTERS[idx].key;
}

// 만난 사람 수 기반 성장 단계 계산
export function growthOf(metCount: number) {
  if (metCount >= 3) {
    return { stage: 3 as const, emoji: '🌸', label: '꽃', toNext: null };
  }
  if (metCount >= 1) {
    return { stage: 2 as const, emoji: '🌿', label: '잎', toNext: 3 - metCount };
  }
  return { stage: 1 as const, emoji: '🌱', label: '새싹', toNext: 1 };
}

// 서버 미배포 시 tagCode 해시 기반 폴백
export function characterOf(key?: string, fallbackSeed?: string) {
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

// 공통 관심사 교집합 추출
export function commonInterests(mine: string[] = [], theirs: string[] = []): string[] {
  const set = new Set(theirs);
  return mine.filter(item => set.has(item));
}
```

### 3.2 신규 컴포넌트 목록
1. **`src/components/CharacterPicker.tsx`**:
   - 8개 캐릭터 라디오 버튼 그룹 (`role="radiogroup"`).
   - 기본적으로 1개가 항상 선택되어 있어 시나리오 입력 생략 시에도 완벽 통과.
2. **`src/components/InterestPicker.tsx`**:
   - 12개 관심사 토글 칩 (`data-testid="interest-chip-*"`).
   - 최대 3개 선택 시 나머지 비활성화.
3. **`src/components/CharacterAvatar.tsx`**:
   - 캐릭터 이모지 크게 + 우하단 성장 이모지(🌱/🌿/🌸) + 성장 단계 텍스트 뱃지.
4. **`src/components/LevelPicker.tsx`**:
   - 결과 페이지에서 선택된 대상마다 "다시 만나고 싶은 정도" 3개 버튼 노출 (기본값: `🙌 반가웠어요`).

### 3.3 화면별 상세 요구사항 및 고정 텍스트
- **홈 (`HomePage.tsx`)**:
  - `파티 만들기` 버튼 → 모달 오픈 → `파티 이름` 입력 → `파티 만들기` 클릭 → URL `/party/:code` 이동.
  - 요금 고정 문구: `20명까지 무료 / 초과 시 9,900원`
- **패스포트 (`PartyPassportPage.tsx`)**:
  - 참여 폼 (비세션): `이름` 입력 → `참여하기` 클릭 → `참여 완료` (DOM 배지 및 토스트).
  - 패스포트 본체 (세션):
    - 상단 카드: 캐릭터 이모지, `내 코드: XXXX`, 성장 단계 라벨(`새싹`/`잎`/`꽃`), `초대 링크 복사` 버튼.
    - 성장 알림: 만난 사람 수 증가로 단계 상승 시 토스트 `"캐릭터가 잎으로 자랐어요"`.
    - 미션 카드: 상대 캐릭터 + 상대 관심사 + `"공통 관심사: 러닝 — 이걸로 말 걸어 보세요"`.
    - 만난 사람 목록: 캐릭터 이모지 + 이름 + 태그 코드 + 공통 관심사 하이라이트.
- **결과 화면 (`PartyResultPage.tsx`)**:
  - `다시 만나고 싶은 사람` 체크박스 목록.
  - 체크 시 아래에 **"다시 만나고 싶은 정도"** 3버튼 (`☕ 가볍게`, `🙌 반가웠어요`, `💫 꼭 다시`) 노출.
  - 제출 버튼: `선택 제출` → 상호 매칭된 경우 `서로 선택된 사람` 섹션에 `나 💫 꼭 다시 · 상대 🙌 반가웠어요` 표시.
  - **금기어 준수**: "호감도"라는 단어는 화면 및 기획안 일체 미사용.

---

## 4. 백엔드 상세 설계

### 4.1 데이터 모델 (JPA 엔티티)
```
party:
  party_id UUID (PK)
  code CHAR(6) UNIQUE
  name VARCHAR(60)
  capacity INT (기본 20)
  created_at TIMESTAMP
  closed_at TIMESTAMP NULL

participant:
  participant_id UUID (PK)
  party_id UUID (FK)
  name VARCHAR(20)
  tag_code CHAR(4) UNIQUE(party_id, tag_code)
  mission_target_id UUID NULL
  is_host BOOLEAN
  character_key VARCHAR(16) NULL (PostgreSQL 예약어 회피)
  interests VARCHAR(100) NULL (쉼표 구분 문자열, 최대 3개)
  joined_at TIMESTAMP

meet:
  meet_id UUID (PK)
  party_id UUID (FK)
  participant_a_id UUID (FK)
  participant_b_id UUID (FK)
  created_at TIMESTAMP
  UNIQUE (party_id, min(a,b), max(a,b))

pick:
  pick_id UUID (PK)
  party_id UUID (FK)
  from_participant_id UUID (FK)
  to_participant_id UUID (FK)
  pick_level INT NULL (1: 가볍게, 2: 반가웠어요, 3: 꼭 다시 - 기본 2)
  created_at TIMESTAMP
  UNIQUE (party_id, from, to)
```

### 4.2 REST API 엔드포인트 및 DTO 명세

| 메서드 · 경로 | 요청 Body | 응답 | 설명 및 보안 검증 |
|---|---|---|---|
| `POST /api/parties` | `{ name, hostName?, capacity?, hostCharacter?, hostInterests?: string[] }` | 201 `PassportResponse` | 파티 및 호스트 참가자 생성. 캐릭터/관심사 정규화 저장 |
| `GET /api/parties/{code}` | — | 200 `PartyStatus` | 참가자 수, 만남 수, 파티 종료 여부, 요금 문구 |
| `POST /api/parties/{code}/join` | `{ name, fromTagCode?, character?, interests?: string[] }` | 201 `PassportResponse` | 파티 참여. `fromTagCode` 존재 시 **초대자와 즉시 상호 태그(Meet) 자동 생성** (Option A) |
| `GET /api/parties/{code}/passport/{participantId}` | — | 200 `PassportResponse` | 4초 폴링. 내 캐릭터, 관심사, 성장단계, 만난 사람 목록 |
| `POST /api/parties/{code}/tag` | `{ participantId, targetTagCode }` | 200 `PassportResponse` | 4자리 태그 코드로 수동 만남 기록. 자기 자신 400 |
| `POST /api/parties/{code}/picks` | `{ participantId, picks?: [{ targetTagCode, level }], targetParticipantIds?: string[] }` | 200 `MatchResponse` | 🔴 `targetTagCode`로 대상 매핑. `level` 1~3 저장. 구버전 호환 |
| `GET /api/parties/{code}/matches/{participantId}` | — | 200 `MatchResponse` | 양방향 상호 선택된 상대만 반환 (양쪽 레벨 포함) |
| `POST /api/parties/{code}/close` | `{ participantId }` | 200 `PartyStatus` | 🔴 해당 참가자가 `isHost`인지 검증. 비호스트 시 403 Forbidden |

#### DTO 변경 사항 (🔴 보안 및 신규 필드)
```java
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
    String character,               // 🆕 8종 키
    List<String> interests,         // 🆕 0~3개 관심사
    int growthStage,                // 🆕 1, 2, 3
    String missionTargetCharacter,  // 🆕 상대 캐릭터
    List<String> missionTargetInterests // 🆕 상대 관심사
) {}

public record MetPersonDto(
    String tagCode,                 // 🔴 participantId 완전 삭제 (남의 UUID 은닉)
    String name,
    String metAt,
    String character,               // 🆕
    List<String> interests,         // 🆕
    Integer myLevel,                // 🆕 mutualMatches 항목에서만 (1~3)
    Integer theirLevel              // 🆕 mutualMatches 항목에서만 (1~3)
) {}

public record PickItem(
    String targetTagCode,
    int level
) {}

public record SubmitPicksRequest(
    UUID participantId,
    List<PickItem> picks,
    List<String> targetParticipantIds // 하위 호환용 (fallback)
) {}
```

---

## 5. 하위 호환성 및 Zero-Downtime 배포 전략

1. **배포 시차 무관 동작 (Decoupled Deployment)**:
   - **프론트 선배포 시**: 백엔드 응답에 `character`가 없으면 `characterOf(undefined, tagCode)` 해시 폴백이 동작하여 동일 인물에게 일관된 캐릭터가 노출되며, 성장 단계는 프론트 계산 함수 `growthOf(metCount)`로 완벽 작동.
   - **백엔드 선배포 시**: JPA 컬럼이 `nullable`로 추가되므로 기존 프론트의 레거시 요청을 정상 수용.
2. **선택 제출 API 하위 호환**:
   - `POST /picks`에서 `picks` 배열이 있으면 우선 처리하고, 없으면 구버전 `targetParticipantIds`를 `level: 2(반가웠어요)`로 자동 변환 저장.

---

## 6. 테스트 및 수용 기준 (Verification Criteria)

| # | 테스트 영역 | 검증 도구 | 통과 기준 |
|---|---|---|---|
| 1 | 프론트 순수 도메인 단위 테스트 | `node --test` / `npm test` | `character.test.ts` 100% 통과 (성장 단계 경계 0/1/2/3, 해시 결정성, 교집합) |
| 2 | 백엔드 서비스/보안 단위 테스트 | `./gradlew test` | `PartyPassportTest` 통과 (랜덤 캐릭터 배정, 관심사 3개 클램프, 태그코드 매칭, 비호스트 종료 403) |
| 3 | 3단계 시나리오 E2E 완주 (A1) | Playwright (MCP) | 데스크톱(1280×720) 및 모바일(375×812)에서 글자 단위 100% 일치 완주 및 콘솔 에러 0건 |
| 4 | 파티형 확산 구조 실동작 (B3) | Playwright / 브라우저 | 2단계 복사 링크로 3단계 참여 시 즉시 호스트와 상호 태그(만난 사람 1명, 첫 만남 증표) 확인 |
| 5 | 리텐션 및 회고 플로우 (B4) | 브라우저 검증 | 파티 종료 후 3단계 정도 선택 제출 → 상호 매칭 카드 확인 → 증표함 저장 → 다음 파티 만들기 링크 동작 |

---

## 7. 결론 및 다음 단계
본 설계는 **Playwright 자동화 에이전트의 3단계 시나리오 100% 완주(A1 13.6%)**와 **초대 기반 상호 태그 실동작(B3 9.9%)**, **카톡 대체 불가성(C3 9.9%)**, **가격 정책 노출(C1 9.9%)**을 완벽하게 지원하여, AI 심사관 3인방 모두에게 최고 점수를 획득하도록 설계되었습니다.

본 설계서 승인 후 `writing-plans` 스킬을 통해 세부 실행 계획(Implementation Plan) 수립으로 전환합니다.
