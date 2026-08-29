# PRD — 파티 패스포트 · 백엔드 (Firestore + 보안 규칙 + 익명 인증)

> 서버 코드 0줄. 백엔드 = Firestore 컬렉션 5개 + 규칙 파일 1개 + 익명 인증 토글. 프론트 계약은 [frontend.md](./frontend.md), 제품 근거는 [thinking/파티패스포트.md](../thinking/파티패스포트.md) v3.

## 1. 원칙

| 원칙 | 이유 |
|---|---|
| 서버 코드 0줄 (Cloud Functions 없음) | Functions는 유료 플랜(Blaze). 24h 종료·상호 판정 전부 클라이언트 + 규칙 |
| **규칙이 강제하는 것만 문서에 "보장"이라고 쓴다** | 리뷰 지적: `allow write: if true`로 "비밀 보장"을 주장했음. v3는 익명 uid로 소유권을 판정 |
| 읽기는 구독(`onSnapshot`), 폴링 금지 | 3초 폴링은 20명 파티에서 무료 티어를 2분에 소진 (thinking 9절). 예외: 상호 결과는 규칙상 구독 불가 → 30초·내 선택만 |
| 로그인 화면 없음 | 심사 규칙 4. 익명 인증은 화면에 보이지 않는다 |

## 2. 인증 — Firebase Anonymous Auth

- 콘솔: Authentication → Sign-in method → **익명(Anonymous) 사용 설정**.
- 클라이언트: 앱 부팅 시 `signInAnonymously(auth)` → `onAuthStateChanged`로 uid 확보 후 렌더. 브라우저에 유지(IndexedDB) → 재방문 시 같은 uid.
- 의미: **uid = 브라우저.** 사람이 아니다. 한 uid가 한 파티에 참가자를 여러 명 가질 수 있다(심사 에이전트가 탭 하나로 호스트→참가자를 연달아 함). 규칙은 "이 참가자 문서를 만든 uid인가"만 본다.
- 실패 처리: 프론트가 `연결에 실패했어요 · 다시 시도`. 규칙은 전부 `request.auth != null`을 요구하므로 인증 없이는 아무것도 읽고 쓸 수 없다.

## 3. 데이터 모델

```
parties/{partyId}                      { name: string(1..40), size: int(2..500), hostUid: string,
                                         createdAt: int(ms), endedAt: int(ms) | null }
parties/{partyId}/participants/{pid}   { name: string(1..20), uid: string, joinedAt: int(ms) }
parties/{partyId}/codes/{code}         { pid: string }
parties/{partyId}/meets/{a_b}          { a: pid, b: pid, code: string, at: int(ms) }   // a < b
parties/{partyId}/picks/{a_b}          { [pid]: true }                                   // 키 1~2개
```

| 항목 | 규칙·불변식 |
|---|---|
| `partyId` | 6자, `pid` 8자, `code` 4자 — 문자 집합 `ABCDEFGHJKMNPQRSTUVWXYZ23456789`(혼동 문자 제외). 클라이언트 생성 |
| `codes/{code}` | **참가자의 비밀.** `get`만 허용, `list` 금지 → 코드를 아는 사람만 pid를 알 수 있다. 참가자 문서에는 코드를 두지 않는다(문서는 전부 읽히므로) |
| `meets/{a_b}` | 문서 ID = 사전순 쌍. `a < b`라 자기 자신 불가, 같은 쌍은 한 문서 → 중복 불가. `code`는 **요청자가 아닌 쪽**의 코드여야 한다 |
| `picks/{a_b}` | 키 = pid. 생성 시 키 1개(자기 것), 갱신은 상대 키 1개 추가만. 키 2개 = 상호 |
| 시간 | 전부 epoch ms 정수(`Date.now()`) — 규칙에서 `request.time.toMillis()`와 비교 |
| 종료 | `endedAt != null` 또는 `now > createdAt + 86_400_000`. 서버 작업 없음 |

## 4. 보안 규칙 (`firestore.rules`) — 전문

```
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {

    function signedIn() { return request.auth != null; }
    function partyOf(party) { return get(/databases/$(db)/documents/parties/$(party)).data; }
    function isOpen(party) {
      return partyOf(party).endedAt == null
          && request.time.toMillis() < partyOf(party).createdAt + 86400000;
    }
    function ownsPid(party, pid) {
      return get(/databases/$(db)/documents/parties/$(party)/participants/$(pid)).data.uid == request.auth.uid;
    }
    function pidOfCode(party, code) {
      return get(/databases/$(db)/documents/parties/$(party)/codes/$(code)).data.pid;
    }
    function pa(pair) { return pair.split('_')[0]; }
    function pb(pair) { return pair.split('_')[1]; }
    // 갱신이 "키 pid 하나만 추가"인지
    function addsOnly(pid) {
      return request.resource.data.diff(resource.data).addedKeys().hasOnly([pid])
          && request.resource.data.diff(resource.data).addedKeys().size() == 1
          && request.resource.data.diff(resource.data).removedKeys().size() == 0
          && request.resource.data.diff(resource.data).changedKeys().size() == 0;
    }

    match /parties/{party} {
      allow get, list: if signedIn();
      allow create: if signedIn()
        && request.resource.data.hostUid == request.auth.uid
        && request.resource.data.name is string && request.resource.data.name.size() >= 1 && request.resource.data.name.size() <= 40
        && request.resource.data.size is int && request.resource.data.size >= 2 && request.resource.data.size <= 500
        && request.resource.data.endedAt == null;
      // 호스트만, endedAt만 바꿀 수 있다 (파티 종료)
      allow update: if signedIn()
        && resource.data.hostUid == request.auth.uid
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['endedAt']);
      allow delete: if false;

      match /participants/{pid} {
        allow get, list: if signedIn();
        allow create: if signedIn()
          && request.resource.data.uid == request.auth.uid
          && request.resource.data.name is string && request.resource.data.name.size() >= 1 && request.resource.data.name.size() <= 20
          && isOpen(party);
        allow update, delete: if false;
      }

      match /codes/{code} {
        allow get: if signedIn();          // 코드를 아는 사람만 조회
        allow list: if false;              // 열거 금지
        allow create: if signedIn() && ownsPid(party, request.resource.data.pid);   // 이미 있으면 create 실패 → 유일성
        allow update, delete: if false;
      }

      match /meets/{pair} {
        allow get, list: if signedIn();
        // 정렬된 쌍 + 요청자가 쌍의 한쪽 + 첨부한 코드가 "상대" 것 + 파티 진행 중
        allow create: if signedIn()
          && isOpen(party)
          && request.resource.data.a < request.resource.data.b
          && pair == request.resource.data.a + '_' + request.resource.data.b
          && (
               (ownsPid(party, request.resource.data.a) && pidOfCode(party, request.resource.data.code) == request.resource.data.b)
            || (ownsPid(party, request.resource.data.b) && pidOfCode(party, request.resource.data.code) == request.resource.data.a)
          );
        allow update, delete: if false;
      }

      match /picks/{pair} {
        allow list: if false;
        // 둘 다 골랐을 때만, 쌍의 당사자만 읽는다
        allow get: if signedIn()
          && resource.data.size() == 2
          && (ownsPid(party, pa(pair)) || ownsPid(party, pb(pair)));
        // 종료 후 · 만난 쌍만 · 자기 키 하나로 생성
        allow create: if signedIn()
          && !isOpen(party)
          && exists(/databases/$(db)/documents/parties/$(party)/meets/$(pair))
          && request.resource.data.size() == 1
          && (
               (request.resource.data.keys().hasOnly([pa(pair)]) && ownsPid(party, pa(pair)))
            || (request.resource.data.keys().hasOnly([pb(pair)]) && ownsPid(party, pb(pair)))
          );
        // 상대가 먼저 골랐으면 자기 키 하나만 추가 (취소·수정 불가)
        allow update: if signedIn()
          && !isOpen(party)
          && resource.data.size() == 1 && request.resource.data.size() == 2
          && (
               (addsOnly(pa(pair)) && ownsPid(party, pa(pair)))
            || (addsOnly(pb(pair)) && ownsPid(party, pb(pair)))
          );
        allow delete: if false;
      }
    }
  }
}
```

### 4.1 규칙이 보장하는 것 / 못 하는 것

| 보장 (규칙 강제) | 한계 (솔직하게 문서에 쓴다) |
|---|---|
| 참가자·코드·만남·선택은 **인증된 브라우저만** 쓴다 | uid는 사람이 아니라 브라우저. 시크릿 창 = 새 사람 |
| 만남은 **상대 코드가 맞아야** 생성 — 참가자 목록·devtools로 남을 태그 못 함 | 코드를 받은 뒤 실제 대화했는지는 검증 못 함(자기 확인). 초대 링크에 코드가 실리므로 원격으로도 가능 → 프론트가 자동 기록 대신 `지금 만났어요` 확인을 둠 |
| 종료된 파티에는 만남·참여 불가, 선택은 종료 후에만 | 24h 판정은 `request.time` 기준 — 클라이언트 시계와 최대 수 초 차이 |
| 선택은 자기 키만 · 한 번만 · 만난 쌍만. 삭제·수정 불가 → **"골라 보고 빼기"로 짝사랑 탐지 불가** | 한 uid가 참가자 둘을 가지면 그 둘끼리 상호 선택 가능(심사 에이전트 케이스, 실사용에선 자기 자신) |
| 결과는 키 2개일 때만 `get`, `list` 금지 → 운영자·개발자·devtools 누구도 단독 선택을 못 봄 | 규칙 `get()` 호출은 읽기로 과금(6절 반영) |
| 파티 문서는 호스트 uid만, `endedAt`만 수정 | 호스트가 폰을 바꾸면 종료 못 함 → 24h 자동 종료가 안전망 |

## 5. 클라이언트 API 계약 (`src/api/party.ts`)

모든 함수는 `throw`로 실패를 알린다(프론트 `useMutation`이 잡아 문구로 표시). 시간은 `Date.now()`.

| 함수 | 쓰기 순서 | 실패·주의 |
|---|---|---|
| `createParty({ name, hostName, size, uid }) → { party, me }` | ① `parties/{id}` ② `participants/{pid}` (host, uid) ③ `codes/{code}` — 충돌 시 코드 재생성 후 ③ 재시도(최대 5회) | 배치 쓰기 금지: 규칙 `get()`이 배치 이전 상태를 보므로 ②→③은 순차 |
| `joinParty(partyId, { name, uid }) → { pid, code }` | ① `participants/{pid}` ② `codes/{code}` (충돌 재시도) | 종료된 파티면 규칙이 ①을 거부 → `종료된 파티예요` |
| `resolveCode(partyId, code) → pid \| null` | `getDoc(codes/{code})` | 없으면 `null` (`그 코드는 없어요`) |
| `addMeet(partyId, me: pid, other: pid, otherCode)` | `setDoc(meets/{pairId})` `{ a, b, code: otherCode, at }` | `me === other` 거부는 프론트에서 먼저(`내 코드예요`); 이미 있으면 규칙이 `update`로 보고 거부 → `이미 만났어요` |
| `endParty(partyId)` | `updateDoc(parties/{id}, { endedAt })` | 호스트 uid 아니면 거부 |
| `submitPicks(partyId, me, others: pid[])` | 각 상대에 대해 `picks/{pairId}`: 문서 없으면 `setDoc({[me]: true})`, 있으면 `updateDoc({[me]: true})`. 존재 여부는 `get`이 거부될 수 있으므로 **`setDoc` 시도 → 실패 시 `updateDoc`** 순서 | 제출 완료는 `localStorage['picked:<party>:<pid>']`로 기억(목록 조회 불가) |
| `checkMutual(partyId, me, other) → boolean` | `getDoc(picks/{pairId})` — 성공 = 상호 | `permission-denied` = 아직 아님(정상 경로, 에러 표시 금지) |
| 구독 | `onSnapshot(parties/{id})`, `onSnapshot(participants)`, `onSnapshot(meets)` | `picks`는 구독 불가(규칙) |

## 6. 비용·용량 (C4 숫자의 출처)

파티 1건 = 20명 · 2시간 · 만남 40 · 선택 30 「가정」.

| 항목 | 건수 | 단가 | 비용 |
|---|---|---|---|
| 쓰기: party 1 + participants 20 + codes 20 + meets 40 + picks 30 | ≈ 110 | $0.18 / 10만 | ≈ 0.3원 |
| 구독 초기 읽기: (participants 20 + meets 40) × 20 클라이언트 | 1,200 | $0.06 / 10만 | |
| 구독 증분: 새 문서 60 × 20 리스너 | 1,200 | | |
| 규칙 `get()`: meets 40×4 + codes 20×1 + picks 쓰기 30×3 | ≈ 270 | | |
| 결과 확인: 1인 평균 2건 × 30초 × 10분 × 20명 × (1 + 규칙 get 2) | ≈ 2,400 | | |
| **읽기 합계** | **≈ 5,000** | | **≈ 4원** |
| **파티 1건 서버 원가** | | 환율 1,400원/$「가정」 | **≈ 5원** |
| 무료 티어(Spark): 5만 reads · 2만 writes / 일 | | | **하루 약 10파티 0원** |

Firestore 규칙 제한: 요청당 `get()`/`exists()` 10회(단일 문서), 같은 문서 반복 접근은 1회로 계산 — 위 규칙 최대 4회.

## 7. 운영 체크리스트 (문서 담당 · +0:10)

1. console.firebase.google.com → 프로젝트 추가 → **웹 앱 추가** → `firebaseConfig`를 `src/lib/firebase.ts`에 커밋(공개값. 접근 통제는 규칙이 한다)
2. Firestore Database 만들기 → **프로덕션 모드**, 리전 `asia-northeast3` → **규칙 탭에 4절 전문 붙여넣기 → 게시**
3. Authentication → Sign-in method → **익명 사용 설정**
4. (권장) Authentication → 설정 → **승인된 도메인**에 `twin-fang.github.io` 확인 (기본 포함되지 않으면 추가)
5. 규칙 검증: 콘솔 "규칙 플레이그라운드"로 8절 항목 중 최소 ①③⑤를 확인
6. 제출 후 **배포 프리즈** — 규칙·config 변경 금지. 무료 티어 소진 알림(콘솔 사용량) 확인

## 8. 수용 기준 (규칙 동작)

| # | 시나리오 | 기대 |
|---|---|---|
| ① | 인증 없이 `parties` 읽기/쓰기 | 거부 |
| ② | 같은 브라우저(uid)에서 파티 생성 → 초대 링크 → 두 번째 참가자 생성 → `지금 만났어요` | 전부 허용 (심사 에이전트 경로) |
| ③ | `meets/{a_b}`에 상대 코드 없이/틀린 코드로 생성 | 거부 |
| ④ | `meets/{b_a}`(정렬 안 된 ID) 또는 `a == b` | 거부 |
| ⑤ | 종료 전 `picks` 생성 / 종료 후 만난 적 없는 쌍에 `picks` 생성 | 거부 |
| ⑥ | A가 `picks/{a_b}` `{a: true}` 생성 → A가 `get` | 거부 (키 1개). B가 `{b: true}` 추가 → A·B `get` 허용 |
| ⑦ | A가 `{a: true, b: true}`를 한 번에 생성 | 거부 (size 1) |
| ⑧ | A가 자기 키 삭제/변경, 제3자 C가 `picks/{a_b}` 읽기·쓰기 | 거부 |
| ⑨ | `codes` 컬렉션 `list`, `picks` `list` | 거부 |
| ⑩ | 비호스트 uid가 `endedAt` 갱신, 호스트가 `name` 갱신 | 거부 |

## 9. 알려진 한계 (기획안에 그대로 써도 되는 문장)

- 만남은 "상대 코드 확보 + 본인 확인"이지 대면 검증이 아니다. 증표는 금전 가치가 없어 위조 유인이 낮다.
- 신원은 브라우저에 묶인다. 폰을 바꾸면 증표함·참가자 신원이 이어지지 않는다.
- 상호 선택 결과는 구독이 아니라 30초 확인이다(규칙상 단독 선택 상태의 문서는 읽을 수 없기 때문).
- Cloud Functions가 없어 24h 종료·상호 판정은 클라이언트·규칙의 `request.time`에 의존한다.
