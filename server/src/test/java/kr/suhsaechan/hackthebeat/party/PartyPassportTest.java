package kr.suhsaechan.hackthebeat.party;

import java.util.List;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.*;
import kr.suhsaechan.hackthebeat.party.service.PartyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PartyPassportTest {

    @Autowired
    private PartyService partyService;

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
}
