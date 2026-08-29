# Hack-the-Beat 서버 (Party Passport API)

파티 패스포트 API. 인증 없음, 파티 6자리 코드 및 참가자 4자리 태그 코드로 접근한다.

## 배포 주소

| 구분 | 주소 |
|---|---|
| **API (도메인)** | `https://api.hack-the-beat.suhsaechan.kr` |
| API (포트 직결) | `http://suh-project.synology.me:8096` |
| 헬스체크 | `https://api.hack-the-beat.suhsaechan.kr/health` |

> 프론트가 HTTPS(GitHub Pages)라 브라우저에서 HTTP 주소를 호출하면 혼합 콘텐츠로 차단된다.
> **브라우저에서 부를 때는 반드시 `https://api.hack-the-beat.suhsaechan.kr` 를 쓴다.**

## API 엔드포인트

기준 경로 `/api/parties`. 모든 요청/응답은 JSON.

### 1. 파티 생성 (호스트 패스포트 동시 생성)
- `POST /api/parties`
- Body: `{ "name": "금요일 파티", "hostName": "호스트", "capacity": 30 }`
- Response: `PassportResponse` (201 Created)

### 2. 파티 참여
- `POST /api/parties/{code}/join`
- Body: `{ "name": "김서준", "fromTagCode": "7K2M" }`
- Response: `PassportResponse` (초대자의 `fromTagCode`가 있으면 자동 상호 태그 및 '첫 만남' 증표 지급)

### 3. 패스포트 상태 조회
- `GET /api/parties/{code}/passport/{participantId}`
- Response: `PassportResponse` (만난 사람 수, 진행률, 증표 목록, 미션 상대)

### 4. 4자리 코드로 태그
- `POST /api/parties/{code}/tag`
- Body: `{ "participantId": "uuid", "targetTagCode": "A3B9" }`
- Response: `PassportResponse`

### 5. 파티 종료
- `POST /api/parties/{code}/close`
- Response: `PartyStatus`

### 6. 파티 후 상호 호감 제출 & 결과 조회
- `POST /api/parties/{code}/picks`
- Body: `{ "participantId": "uuid", "targetParticipantIds": ["uuid1", "uuid2"] }`
- Response: `MatchResponse` (양방향 일치하는 상대만 `mutualMatches`로 반환)
- `GET /api/parties/{code}/matches/{participantId}`
- Response: `MatchResponse`

## 로컬 실행 & 빌드

```bash
cd server
./gradlew bootRun
```
