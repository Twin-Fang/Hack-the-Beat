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
@Table(name = "meet", uniqueConstraints = {
    @UniqueConstraint(name = "uk_meet_pair", columnNames = {"party_id", "participant_a_id", "participant_b_id"})
})
public class Meet {

    @Id
    @GeneratedValue
    private UUID meetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_a_id")
    private Participant participantA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_b_id")
    private Participant participantB;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Meet(Party party, Participant participantA, Participant participantB) {
        this.party = party;
        // 항상 participantA <= participantB 순서로 저장하여 무방향 그래프 표현
        if (participantA.getParticipantId().compareTo(participantB.getParticipantId()) <= 0) {
            this.participantA = participantA;
            this.participantB = participantB;
        } else {
            this.participantA = participantB;
            this.participantB = participantA;
        }
        this.createdAt = LocalDateTime.now();
    }
}
