package kr.suhsaechan.hackthebeat.party;

import java.util.List;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.*;
import kr.suhsaechan.hackthebeat.party.service.PartyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PartyPassportTest {

    @Autowired
    private PartyService partyService;

    private static final List<String> ALL_CHARACTERS = List.of(
            "FOX", "FROG", "PANDA", "CHICK", "OCTOPUS", "LION", "RABBIT", "KOALA"
    );

    @Test
    @DisplayName("파티 생성 -> 호스트 패스포트 및 4자리 태그코드 발급")
    void createPartyTest() {
        PassportResponse response = partyService.createParty(new CreatePartyRequest("금요일 파티", "호스트", 30));

        assertThat(response.partyCode()).hasSize(6);
        assertThat(response.partyName()).isEqualTo("금요일 파티");
        assertThat(response.isHost()).isTrue();
        assertThat(response.tagCode()).hasSize(4);
        assertThat(response.metCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("초대 링크로 참여 시 초대자와 즉시 상호 태그(Meet)되고 '첫 만남' 증표 획득")
    void joinWithFromTagCodeTest() {
        // 1. 호스트 파티 생성
        PassportResponse host = partyService.createParty(new CreatePartyRequest("금요일 파티", "호스트", 30));

        // 2. 김서준이 호스트의 fromTagCode로 참여
        PassportResponse guest = partyService.join(host.partyCode(), new JoinRequest("김서준", host.tagCode()));

        // 검증: 게스트의 만난 사람 수가 1명이고 첫 만남 증표가 achieved
        assertThat(guest.name()).isEqualTo("김서준");
        assertThat(guest.metCount()).isEqualTo(1);
        
        BadgeDto firstMeetBadge = guest.badges().stream()
                .filter(b -> b.code().equals("FIRST_MEET"))
                .findFirst().orElseThrow();
        assertThat(firstMeetBadge.achieved()).isTrue();

        // 호스트의 패스포트도 만난 사람 수가 1명으로 증가
        PassportResponse hostUpdated = partyService.getPassport(host.partyCode(), host.participantId());
        assertThat(hostUpdated.metCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("파티 종료 후 상호 선택한 짝만 비밀 매칭 결과로 반환")
    void mutualPicksTest() {
        // 호스트와 게스트 2명
        PassportResponse p1 = partyService.createParty(new CreatePartyRequest("테스트 파티", "A", 20));
        PassportResponse p2 = partyService.join(p1.partyCode(), new JoinRequest("B", p1.tagCode()));
        PassportResponse p3 = partyService.join(p1.partyCode(), new JoinRequest("C", null));

        // P1 -> P2 선택, P2 -> P1 선택 (상호 매칭)
        // P3 -> P1 선택 (단방향 짝사랑)
        partyService.submitPicks(p1.partyCode(), new SubmitPicksRequest(p1.participantId(), List.of(p2.participantId())));
        partyService.submitPicks(p1.partyCode(), new SubmitPicksRequest(p2.participantId(), List.of(p1.participantId())));
        partyService.submitPicks(p1.partyCode(), new SubmitPicksRequest(p3.participantId(), List.of(p1.participantId())));

        // P1 매칭 조회 -> P2만 나와야 함 (P3는 나오면 안 됨)
        MatchResponse p1Matches = partyService.getMatches(p1.partyCode(), p1.participantId());
        assertThat(p1Matches.matchedCount()).isEqualTo(1);
        assertThat(p1Matches.mutualMatches().get(0).name()).isEqualTo("B");

        // P3 매칭 조회 -> 0명 나와야 함
        MatchResponse p3Matches = partyService.getMatches(p1.partyCode(), p3.participantId());
        assertThat(p3Matches.matchedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("캐릭터 8종 및 관심사 12종 유효성 검증, 정규화 및 랜덤 배정 테스트")
    void characterAndInterestsValidationTest() {
        // 1. 호스트: 유효한 캐릭터와 관심사 정상 입력
        PassportResponse host = partyService.createParty(new CreatePartyRequest(
                "캐릭터 파티",
                "호스트",
                20,
                "KOALA",
                List.of("게임", "러닝", "독서")
        ));
        assertThat(host.character()).isEqualTo("KOALA");
        assertThat(host.interests()).containsExactly("게임", "러닝", "독서");

        // 2. 게스트: 무효한 캐릭터("INVALID") -> 8종 중 랜덤 배정
        // 무효한 관심사("수영"), 중복 관심사("게임"), 4개 초과 목록("게임", "수영", "게임", "개발", "카페", "영화") -> 3개 정규화 슬라이스
        PassportResponse guest = partyService.join(host.partyCode(), new JoinRequest(
                "게스트",
                null,
                "INVALID_CHAR",
                List.of("게임", "수영", "게임", "개발", "카페", "영화")
        ));

        assertThat(ALL_CHARACTERS).contains(guest.character());
        assertThat(guest.interests()).containsExactly("게임", "개발", "카페");
    }

    @Test
    @DisplayName("tagCode 및 pickLevel 기반 submitPicks와 상호 매칭 시 myLevel/theirLevel 반환 테스트")
    void picksWithTagCodeAndLevelTest() {
        // 호스트와 게스트 2명
        PassportResponse host = partyService.createParty(new CreatePartyRequest(
                "비밀 선택 파티", "호스트", 20, "FOX", List.of("개발")
        ));
        PassportResponse guest = partyService.join(host.partyCode(), new JoinRequest(
                "게스트", host.tagCode(), "RABBIT", List.of("개발")
        ));

        // 호스트가 게스트에게 tagCode + pickLevel 3 제출
        partyService.submitPicks(host.partyCode(), new SubmitPicksRequest(
                host.participantId(),
                List.of(new PickItem(guest.tagCode(), 3)),
                null
        ));

        // 게스트가 호스트에게 tagCode + pickLevel 1 제출
        partyService.submitPicks(host.partyCode(), new SubmitPicksRequest(
                guest.participantId(),
                List.of(new PickItem(host.tagCode(), 1)),
                null
        ));

        // 호스트 기준 매칭 조회 검증 (myLevel=3, theirLevel=1)
        MatchResponse hostMatches = partyService.getMatches(host.partyCode(), host.participantId());
        assertThat(hostMatches.matchedCount()).isEqualTo(1);
        MetPersonDto hostMatchedPerson = hostMatches.mutualMatches().get(0);
        assertThat(hostMatchedPerson.name()).isEqualTo("게스트");
        assertThat(hostMatchedPerson.tagCode()).isEqualTo(guest.tagCode());
        assertThat(hostMatchedPerson.character()).isEqualTo("RABBIT");
        assertThat(hostMatchedPerson.myLevel()).isEqualTo(3);
        assertThat(hostMatchedPerson.theirLevel()).isEqualTo(1);

        // 게스트 기준 매칭 조회 검증 (myLevel=1, theirLevel=3)
        MatchResponse guestMatches = partyService.getMatches(host.partyCode(), guest.participantId());
        assertThat(guestMatches.matchedCount()).isEqualTo(1);
        MetPersonDto guestMatchedPerson = guestMatches.mutualMatches().get(0);
        assertThat(guestMatchedPerson.name()).isEqualTo("호스트");
        assertThat(guestMatchedPerson.tagCode()).isEqualTo(host.tagCode());
        assertThat(guestMatchedPerson.character()).isEqualTo("FOX");
        assertThat(guestMatchedPerson.myLevel()).isEqualTo(1);
        assertThat(guestMatchedPerson.theirLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("비호스트가 close 호출 시 403 Forbidden 예외 발생, 호스트는 정상 종료")
    void closeAuthorizationTest() {
        PassportResponse host = partyService.createParty(new CreatePartyRequest("권한 테스트 파티", "호스트", 20));
        PassportResponse guest = partyService.join(host.partyCode(), new JoinRequest("게스트", host.tagCode()));

        // 비호스트(게스트)가 파티 종료 시도 -> 403 FORBIDDEN
        assertThatThrownBy(() -> partyService.close(host.partyCode(), guest.participantId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException ex = (ResponseStatusException) e;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getReason()).contains("호스트만 파티를 종료할 수 있습니다");
                });

        // 호스트가 파티 종료 -> 성공
        PartyStatus closedStatus = partyService.close(host.partyCode(), host.participantId());
        assertThat(closedStatus.closed()).isTrue();
    }

    @Test
    @DisplayName("participantId 없이 close 호출 시에도 403 Forbidden (코드만 아는 제3자의 무단 종료 차단)")
    void closeWithoutParticipantIdIsForbiddenTest() {
        PassportResponse host = partyService.createParty(new CreatePartyRequest("무단종료 방지 파티", "호스트", 20));

        assertThatThrownBy(() -> partyService.close(host.partyCode(), null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException ex = (ResponseStatusException) e;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        assertThatThrownBy(() -> partyService.close(host.partyCode(), "  "))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}

