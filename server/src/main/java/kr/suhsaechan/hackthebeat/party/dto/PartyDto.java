package kr.suhsaechan.hackthebeat.party.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import kr.suhsaechan.hackthebeat.party.domain.Mood;

/**
 * 요청·응답 DTO 모음. 파일이 흩어지면 훑기 어려워 한 곳에 중첩 레코드로 둔다.
 */
public final class PartyDto {

    private PartyDto() {
    }

    public record CreatePartyRequest(
            @NotBlank(message = "파티 이름을 입력해주세요") @Size(max = 60) String name,
            @Min(1) @Max(500) Integer capacity
    ) {
    }

    public record JoinRequest(
            @NotBlank(message = "이름을 입력해주세요") @Size(max = 20) String name
    ) {
    }

    public record MoodRequest(
            @NotNull(message = "참여 정보가 없습니다") String participantId,
            @NotNull(message = "기분을 선택해주세요") Mood mood
    ) {
    }

    public record ParticipantResponse(
            String participantId,
            String name
    ) {
    }

    /** 감정 하나의 집계 — 화면 막대 하나에 대응 */
    public record MoodCount(
            String mood,
            String label,
            long count,
            int percent
    ) {
    }

    /** 호스트 개입 알림 */
    public record Alert(
            boolean active,
            String message
    ) {
    }

    /** 호스트 대시보드·참가자 화면이 폴링으로 받아가는 파티 현재 상태 */
    public record PartyStatus(
            String code,
            String name,
            int capacity,
            long participantCount,
            long voteCount,
            List<MoodCount> moods,
            Alert alert,
            boolean closed,
            String priceNotice
    ) {
    }

    /** 파티 종료 후 리포트 — 시간대별 감정 추이 */
    public record TimelinePoint(
            String at,
            Map<String, Long> counts
    ) {
    }

    public record PartyReport(
            String code,
            String name,
            long participantCount,
            long voteCount,
            List<MoodCount> moods,
            List<TimelinePoint> timeline
    ) {
    }
}
