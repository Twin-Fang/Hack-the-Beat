package kr.suhsaechan.hackthebeat.party.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class PartyDto {

    private PartyDto() {
    }

    public record CreatePartyRequest(
            @NotBlank(message = "파티 이름을 입력해주세요") @Size(max = 60) String name,
            String hostName,
            @Min(1) @Max(500) Integer capacity,
            String hostCharacter,
            List<String> hostInterests
    ) {
        public CreatePartyRequest(String name, String hostName, Integer capacity) {
            this(name, hostName, capacity, null, null);
        }
    }

    public record JoinRequest(
            @NotBlank(message = "이름을 입력해주세요") @Size(max = 20) String name,
            String fromTagCode,
            String character,
            List<String> interests
    ) {
        public JoinRequest(String name, String fromTagCode) {
            this(name, fromTagCode, null, null);
        }
    }

    public record CloseRequest(
            String participantId
    ) {
    }

    public record TagRequest(
            @NotBlank(message = "내 참여자 정보가 필요합니다") String participantId,
            @NotBlank(message = "태그할 4자리 코드를 입력해주세요") @Size(min = 4, max = 4) String targetTagCode
    ) {
    }

    public record PickItem(
            @NotBlank String targetTagCode,
            Integer level
    ) {
        public PickItem(String targetTagCode) {
            this(targetTagCode, 2);
        }
    }

    public record SubmitPicksRequest(
            @NotBlank(message = "내 참여자 정보가 필요합니다") String participantId,
            List<PickItem> picks,
            List<String> targetParticipantIds
    ) {
        public SubmitPicksRequest(String participantId, List<String> targetParticipantIds) {
            this(participantId, null, targetParticipantIds);
        }
    }

    public record BadgeDto(
            String code,
            String title,
            String description,
            boolean achieved
    ) {
    }

    public record MetPersonDto(
            String name,
            String tagCode,
            String metAt,
            String character,
            List<String> interests,
            Integer myLevel,
            Integer theirLevel
    ) {
        public MetPersonDto(String name, String tagCode, String metAt) {
            this(name, tagCode, metAt, null, List.of(), null, null);
        }
    }

    public record PassportResponse(
            String partyCode,
            String partyName,
            String participantId,
            String name,
            String tagCode,
            boolean isHost,
            boolean isClosed,
            int metCount,
            long totalParticipants,
            int progressPercent,
            List<BadgeDto> badges,
            List<MetPersonDto> metPersons,
            String missionTargetName,
            boolean missionCleared,
            String priceNotice,
            String character,
            List<String> interests,
            int growthStage,
            String missionTargetCharacter,
            List<String> missionTargetInterests
    ) {
    }

    public record PartyStatus(
            String code,
            String name,
            int capacity,
            long participantCount,
            long meetCount,
            boolean closed,
            String priceNotice
    ) {
    }

    public record MatchResponse(
            String participantId,
            String name,
            int matchedCount,
            List<MetPersonDto> mutualMatches,
            List<MetPersonDto> allMetPersons,
            boolean reunionBadgeAchieved,
            String picksDeadline
    ) {
    }
}
