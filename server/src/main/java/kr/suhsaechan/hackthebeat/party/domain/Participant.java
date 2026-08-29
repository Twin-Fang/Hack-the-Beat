package kr.suhsaechan.hackthebeat.party.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "participant", indexes = {
    @Index(name = "idx_participant_party_tag", columnList = "party_id, tag_code")
})
public class Participant {

    @Id
    @GeneratedValue
    private UUID participantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    private Party party;

    @Column(nullable = false, length = 20)
    private String name;

    /** 4자리 고유 태그 코드 (예: 7K2M) - QR 및 직접 태그 입력 시 사용 */
    @Column(name = "tag_code", nullable = false, length = 4)
    private String tagCode;

    /** 1:1 미션 상대 participantId (직전 참여자 또는 확정된 대상) */
    @Column(name = "mission_target_id")
    private UUID missionTargetParticipantId;

    @Column(name = "is_host", nullable = false)
    private boolean isHost;

    @Column(name = "character_key", length = 16)
    private String characterKey;

    @Column(length = 100)
    private String interests;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @Builder
    private Participant(Party party, String name, String tagCode, UUID missionTargetParticipantId, boolean isHost, String characterKey, String interests) {
        this.party = party;
        this.name = name;
        this.tagCode = tagCode;
        this.missionTargetParticipantId = missionTargetParticipantId;
        this.isHost = isHost;
        this.characterKey = characterKey;
        this.interests = interests;
        this.joinedAt = LocalDateTime.now();
    }

    public void updateMissionTarget(UUID targetId) {
        if (this.missionTargetParticipantId == null) {
            this.missionTargetParticipantId = targetId;
        }
    }
}
