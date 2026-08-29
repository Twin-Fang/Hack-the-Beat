# Hack-the-Beat 서버 (PARTY MOOD API)

파티 감정 온도계 API. 인증 없음, 파티 코드만으로 접근한다.

## 배포 주소

| 구분 | 주소 |
|---|---|
| **API (도메인)** | `https://api.hack-the-beat.suhsaechan.kr` |
| API (포트 직결) | `http://suh-project.synology.me:8096` |
| 헬스체크 | `https://api.hack-the-beat.suhsaechan.kr/health` |

> 프론트가 HTTPS(GitHub Pages)라 브라우저에서 HTTP 주소를 호출하면 혼합 콘텐츠로 차단된다.
> **브라우저에서 부를 때는 반드시 `https://api.hack-the-beat.suhsaechan.kr` 를 쓴다.**
> 포트 직결 주소는 서버 점검·curl 테스트용이다.

## 인증·보안

**없다.** 로그인·토큰·권한 검사가 전혀 없고 CORS는 모든 오리진·메서드·헤더에 열려 있다.
파티 6자리 코드를 아는 사람은 누구나 조회·참여·기록할 수 있다.

## 스택

Spring Boot 3.4.1 · Java 17 · Spring Data JPA · PostgreSQL(`hackthebeat` DB, NAS `postgres` 컨테이너)

## API

기준 경로 `/api/parties`. 모든 응답은 JSON.

### 1. 파티 생성

```
POST /api/parties
{ "name": "여름 워크샵", "capacity": 40 }
→ 201 PartyStatus
```

`code`(6자리)가 초대 링크 경로가 된다. `capacity` 생략 시 30.

### 2. 파티 상태 조회 (호스트 대시보드·참가자 화면 폴링)

```
GET /api/parties/{code}
→ 200 PartyStatus
```

### 3. 참가자 등록 (초대 링크로 진입)

```
POST /api/parties/{code}/participants
{ "name": "김서준" }
→ 201 { "participantId": "uuid", "name": "김서준" }
```

### 4. 감정 기록

```
POST /api/parties/{code}/moods
{ "participantId": "uuid", "mood": "AWKWARD" }
→ 200 PartyStatus
```

`mood`: `FUN`(재밌다) · `AWKWARD`(어색하다) · `HUNGRY`(배고프다) · `QUIET`(조용했으면)

### 5. 파티 종료

```
POST /api/parties/{code}/close
→ 200 PartyStatus (closed: true)
```

### 6. 리포트 (오늘의 온도 그래프)

```
GET /api/parties/{code}/report
→ 200 { code, name, participantCount, voteCount, moods[], timeline[] }
```

`timeline`은 5분 단위 버킷의 감정별 건수.

### PartyStatus 형태

```json
{
  "code": "SJKSMV",
  "name": "여름 워크샵",
  "capacity": 40,
  "participantCount": 2,
  "voteCount": 2,
  "moods": [
    { "mood": "FUN", "label": "재밌다", "count": 0, "percent": 0 },
    { "mood": "AWKWARD", "label": "어색하다", "count": 2, "percent": 100 },
    { "mood": "HUNGRY", "label": "배고프다", "count": 0, "percent": 0 },
    { "mood": "QUIET", "label": "조용했으면", "count": 0, "percent": 0 }
  ],
  "alert": { "active": true, "message": "\"어색하다\"가 2개 중 2개입니다 — 게임 하나 돌릴 시점입니다" },
  "closed": false,
  "priceNotice": "30명까지 무료 / 초과 시 9,900원"
}
```

- `alert.active`: 참가자 2명 이상 + '어색하다' 비율 50% 이상이면 `true`. 호스트 화면의 개입 알림에 그대로 쓴다
- `priceNotice`: 파티 생성 화면 요금 안내 문구 (결제 없음 — C1 근거를 화면에 남기기 위한 표시)
- 없는 코드는 404, 잘못된 요청은 400

## 로컬 실행

```bash
cd server
./gradlew bootRun          # 기본값은 localhost:5432/hackthebeat
```

DB 접속 정보는 환경변수로 덮어쓴다: `DB_URL` · `DB_USERNAME` · `DB_PASSWORD`

## 배포

`main`에 `server/**` 변경이 push되면 `.github/workflows/PROJECT-SERVER-CICD.yaml`이
Gradle 빌드 → Docker Hub(`cassiiopeia/hack-the-beat-back-container`) 푸시 → NAS 컨테이너 교체 →
헬스체크까지 자동으로 수행한다.

| 항목 | 값 |
|---|---|
| 컨테이너 | `hack-the-beat-back` |
| 포트 | `8096` → 컨테이너 8080 |
| 네트워크 | `postgres_default` (DB 호스트명 `postgres`) |
| 재시작 정책 | `unless-stopped` |
| 도메인 연결 | DSM 역방향 프록시 `api.hack-the-beat.suhsaechan.kr:443` → `localhost:8096` |

필요한 GitHub Actions 시크릿: `DOCKERHUB_USERNAME` `DOCKERHUB_TOKEN` `SERVER_HOST` `SERVER_USER` `SERVER_PASSWORD` `DB_URL` `DB_USERNAME` `DB_PASSWORD` (등록 완료)
