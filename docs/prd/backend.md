# PRD — 파티 패스포트 · 백엔드 (Spring Boot REST API)

> 실제 구현 기준(`server/`, 배포 `https://api.hack-the-beat.suhsaechan.kr`). 제품 근거는 [thinking/파티패스포트.md](../thinking/파티패스포트.md) v4, 프론트 계약은 [frontend.md](./frontend.md). 엔드포인트 요약은 [server/README.md](../../server/README.md).
> **신규 요구사항은 🆕, 수정 필수는 🔴, 결정 대기는 ⬜** 로 표시한다. 표시 없는 항목은 현재 동작.

## 1. 스택·배포

| 항목 | 값 |
|---|---|
| 런타임 | Spring Boot 3.4 · Java 17 · Spring Data JPA · PostgreSQL (`ddl-auto: update`) |
| 배포 | 팀원(suhsaechan) NAS, Docker(`server/Dockerfile`, `app.jar`). HTTPS 도메인 필수 — GitHub Pages(HTTPS)에서 HTTP 호출은 혼합 콘텐츠로 차단 |
| 인증 | 없음. **참가자 ID(UUID) = 본인 토큰**, 파티 6자리 코드 · 참가자 4자리 태그 코드로 접근 |
| 테스트 | `./gradlew test` — H2 인메모리(`src/test/resources/application.yml`), `PartyPassportTest` |
| 재배포 | 서버 변경은 소유자가 `bootJar` → Docker 재빌드. 컬럼 추가는 `ddl-auto: update`로 자동(nullable만) |

## 2. 데이터 모델

```
party        party_id UUID · code CHAR(6) unique · name VARCHAR(60) · capacity INT · created_at · closed_at NULL
participant  participant_id UUID · party_id · name VARCHAR(20) · tag_code CHAR(4) (party 내 unique) · mission_target_id UUID NULL · is_host · joined_at
             🆕 character_key VARCHAR(16) NULL · 🆕 interests VARCHAR(100) NULL (쉼표 구분, 최대 3개)
meet         meet_id · party_id · participant_a_id · participant_b_id · created_at   — a ≤ b 정렬, (party, a, b) unique
pick         pick_id · party_id · from_participant_id · to_participant_id · created_at — (party, from, to) unique
             🆕 pick_level INT NULL (1 가볍게 · 2 반가웠어요 · 3 꼭 다시, NULL = 2)
```

| 불변식 | 구현 |
|---|---|
| 태그 코드는 파티 내 유일, 문자 집합 `ABCDEFGHJKMNPQRSTUVWXYZ23456789` | `generateUniqueTagCode` 20회 재시도 |
| 만남은 무방향·중복 불가·자기 자신 불가 | `Meet` 빌더가 정렬, unique 제약, 서비스가 self 거부 |
| 미션 상대 = 직전 참여자, 호스트는 2번 참여자. 한 번 정해지면 불변 | `join()`에서 확정, `updateMissionTarget`은 null일 때만 |
| 🆕 캐릭터 키는 8종 중 하나. 없거나 다르면 서버가 랜덤 배정 | `FOX FROG PANDA CHICK OCTOPUS LION RABBIT KOALA` |
| 🆕 관심사는 12종 목록 안에서만, 중복 제거, 3개 초과는 앞 3개 | `게임 러닝 영화 음악 여행 요리 독서 그림 축구 반려동물 카페 개발` |
| 🆕 정도는 1~3으로 clamp, 없으면 2 | |
| `character` 는 PostgreSQL 예약어 → 컬럼명 `character_key` | |

## 3. API

기준 경로 `/api/parties`, JSON. 오류는 `ResponseStatusException` 메시지(한국어)를 본문으로 — 프론트가 그대로 표시한다.

| # | 메서드 · 경로 | 요청 | 응답 | 비고 |
|---|---|---|---|---|
| 1 | `POST /` | `{ name, hostName?, capacity? , 🆕 hostCharacter?, 🆕 hostInterests?: string[] }` | 201 `PassportResponse` | 호스트 참가자 동시 생성. `capacity` 기본 20 |
| 2 | `GET /{code}` | — | `PartyStatus` | 참가자·만남 수, 종료 여부, 요금 문구 |
| 3 | `POST /{code}/join` | `{ name, fromTagCode?, 🆕 character?, 🆕 interests?: string[] }` | 201 `PassportResponse` | ⬜ `fromTagCode`가 있으면 초대자와 **자동 상호 태그**(현재). 종료된 파티 400 |
| 4 | `GET /{code}/passport/{participantId}` | — | `PassportResponse` | 프론트가 4초 폴링 |
| 5 | `POST /{code}/tag` | `{ participantId, targetTagCode }` | `PassportResponse` | 코드 없음 404, 자기 자신 400 |
| 6 | `POST /{code}/close` | — | `PartyStatus` | 🔴 호스트 검증 없음 — 아래 5절 |
| 7 | `POST /{code}/picks` | `{ participantId, targetParticipantIds?: string[], 🆕 picks?: [{ targetTagCode, level }] }` | `MatchResponse` | 🔴 대상을 `tagCode`로 바꾼다(5절). `picks`가 있으면 우선, 없으면 `targetParticipantIds`(level 2)로 호환 |
| 8 | `GET /{code}/matches/{participantId}` | — | `MatchResponse` | 상호인 것만 |

### 응답 스키마 변경 (🆕)

```ts
PassportResponse += {
  character: 'FOX' | 'FROG' | 'PANDA' | 'CHICK' | 'OCTOPUS' | 'LION' | 'RABBIT' | 'KOALA'
  interests: string[]                 // 0~3
  growthStage: 1 | 2 | 3              // metCount 0 → 1, 1~2 → 2, 3+ → 3 (프론트도 같은 식으로 계산 가능)
  missionTargetCharacter?: string
  missionTargetInterests?: string[]
}
MetPersonDto += {
  character?: string
  interests?: string[]
  myLevel?: 1 | 2 | 3                 // mutualMatches 항목에서만
  theirLevel?: 1 | 2 | 3              // mutualMatches 항목에서만 — 단독 선택의 정도는 어떤 응답에도 싣지 않는다
}
MetPersonDto -= { participantId }     // 🔴 남의 ID 노출 금지 (5절). 프론트는 tagCode로 대상 지정
```

## 4. 서비스 규칙

| 동작 | 규칙 |
|---|---|
| 파티 생성 | 코드 6자 유일, 호스트 이름 비면 `호스트`, 🆕 캐릭터·관심사 정규화 후 저장 |
| 참여 | 종료 파티 거부 → 참가자 저장(미션 상대 = 직전 참여자) → 직전 참여자 미션 상대 비어 있으면 나로 확정 → ⬜ `fromTagCode` 자동 태그 |
| 태그 | 파티·내 참가자·대상 코드 조회 → self 거부 → 만남 없으면 생성 → 내 패스포트 반환 |
| 패스포트 계산 | 만남 수, 진행률 `met / max(1, total−1)`, 증표 6종, 미션 완료 여부, 🆕 성장 단계, 🆕 미션 상대 캐릭터·관심사 |
| 선택 제출 | 🆕 항목별 `level` 저장(기존 항목은 덮어쓰지 않음 — 취소·수정 불가 유지) → 결과 반환 |
| 결과 | `findMutualMatches`(양방향 존재하는 상대만) → 🆕 각 상대에 대해 내 pick·상대 pick의 level을 붙임 |
| 종료 | `closedAt` 설정, 멱등 |

### ⬜ 결정 대기 — 초대 링크 자동 태그
- **A(현재)**: `join`이 `fromTagCode`로 만남을 즉시 생성. 3단계 시나리오·기획안이 이 동작을 근거로 함.
- **B(권고)**: `join`은 만남을 만들지 않고 `PassportResponse.pendingFromTagCode`(초대자 코드·이름·캐릭터)만 돌려준다. 프론트가 `지금 만났어요`를 누르면 기존 `POST /tag`로 기록. 서버 변경은 자동 태그 블록 삭제 + 응답 필드 1개.

### ⬜ 결정 대기 — 무료 인원
`FREE_CAPACITY` 20(현재) → 50 이면 상수 1줄. 프론트 문구·기획안과 동시에 바꾼다.

## 5. 보안 — 강제하는 것 / 못 하는 것 / 🔴 수정

| 강제 | 한계 |
|---|---|
| 태그는 상대 4자리 코드가 있어야 가능(코드는 본인 화면·QR에만 노출) | 코드 교환 뒤 실제 대화 여부는 검증 불가(자기 신고). ⬜A면 원격 참여도 만남으로 기록 |
| 자기 자신·중복 태그 거부 | 종료 후 태그 거부 없음 — ⬜ `party.isClosed()`면 400, 3줄 |
| 상호 선택은 서버가 양방향 존재할 때만 반환. 단독 선택·정도는 응답·목록 API에 없음 | DB 접근자는 볼 수 있음(운영자 신뢰 범위) |
| 참가자 ID는 UUID v4 — 추측 불가 | 🔴 **만난 사람 응답에 남의 `participantId`가 실린다** → 그 ID로 `/picks`·`/tag`를 남 대신 호출해 가짜 상호 선택을 만들 수 있다 |
| — | 🔴 `/close`는 누구나 호출 가능 — 파티 코드만 알면 종료 |

### 🔴 수정 요구사항 (서버 30줄)
1. `MetPersonDto`에서 `participantId` 제거(프론트는 `tagCode`를 키로 사용).
2. `POST /picks`의 대상을 `targetTagCode`로 받는다(`picks: [{ targetTagCode, level }]`). 같은 파티 안에서 코드 → 참가자 조회.
3. `POST /close`에 `{ participantId }`를 받아 `isHost`인지 검증, 아니면 403.
4. 결과적으로 **남의 참가자 ID를 알 수 있는 경로가 없어진다** → "본인만 자기 선택을 쓸 수 있다"가 성립. 기획안에는 이 문장을 수정 배포 후에만 쓴다.

## 6. 부하·비용 (C4 근거)

| 항목 | 수치 |
|---|---|
| 파티 1건(20명·2시간) 요청 수 | 패스포트 4초 폴링 20명 × 1,800회 = **3.6만 요청**, 나머지(생성·참여·태그·선택) < 200 |
| 요청당 DB | 패스포트 1회 = 조회 4~5쿼리(참가자·만남·상호). 인덱스 `(party_id, tag_code)` 존재 |
| DB 용량 | 참가 20 + 만남 40 + 선택 30 ≈ 90행 ≈ 30KB / 파티 |
| 수용량 | 2vCPU VPS「가정」 초당 200요청 → 동시 파티 50개. 현 NAS는 추가 비용 0원 |
| 건당 비용 | 클라우드 이전 시 월 2만 원 ÷ 월 1,000파티 = **약 20원**「가정」 |
| 부하 절감 여지 | 폴링 4초 → 화면 비활성 시 정지(`refetchIntervalInBackground: false`가 기본), 파티 종료 후 폴링 중단 |

## 7. 운영 체크리스트

1. 컬럼 추가는 nullable로 — `ddl-auto: update`가 기존 데이터를 깨지 않게.
2. 재배포 전 `./gradlew test` 통과(H2). 신규 테스트: 캐릭터 기본 배정 · 관심사 필터(4개 → 3개, 목록 밖 제거) · 정도 clamp · 상호 결과에 양쪽 정도 · 🔴 코드로 선택 제출 · 비호스트 종료 403.
3. 프론트보다 **서버를 먼저** 배포. 프론트는 새 필드가 없어도 동작(하위 호환, frontend PRD 7절).
4. 배포 후 `GET /health` 200, `POST /api/parties` 201 확인.
5. 심사 제출 후 **서버 프리즈** — 재배포·DB 변경 금지.

## 8. 수용 기준 (서비스 테스트)

| # | 시나리오 | 기대 |
|---|---|---|
| ① | 생성(캐릭터 없음) | `character` 8종 중 하나, `interests` 빈 배열, `growthStage` 1 |
| ② | 참여(`character: 'FOX'`, `interests: ['러닝','영화','게임','요리']`) | 캐릭터 FOX, 관심사 3개(앞 3개), 목록 밖 값 제거 |
| ③ | 호스트 코드로 참여(⬜A) | 양쪽 `metCount` 1, 참여자 `growthStage` 2, `FIRST_MEET` 획득 |
| ④ | 태그 후 패스포트 | `metPersons[].character/interests` 채워짐, `participantId` 없음 |
| ⑤ | A→B level 3, B→A level 1 | A 결과 `mutualMatches[0].myLevel` 3 · `theirLevel` 1; C→A 단독 선택은 A·C 어느 응답에도 없음 |
| ⑥ | `picks` 없이 `targetParticipantIds`만 | 호환 동작, level 2 |
| ⑦ | 비호스트 `/close` | 403 |
| ⑧ | 종료된 파티에 참여/태그 | 400 |
