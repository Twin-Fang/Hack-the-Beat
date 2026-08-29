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
@Table(name = "pick", uniqueConstraints = {
    @UniqueConstraint(name = "uk_party_from_to", columnNames = {"party_id", "from_participant_id", "to_participant_id"})
})
public class Pick {

    @Id
    @GeneratedValue
    private UUID pickId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_participant_id")
    private Participant fromParticipant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_participant_id")
    private Participant toParticipant;

    @Column(name = "pick_level")
    private Integer pickLevel;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Pick(Party party, Participant fromParticipant, Participant toParticipant, Integer pickLevel) {
        this.party = party;
        this.fromParticipant = fromParticipant;
        this.toParticipant = toParticipant;
        this.pickLevel = pickLevel != null ? pickLevel : 2;
        this.createdAt = LocalDateTime.now();
    }
}
