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
@Table(name = "participant")
public class Participant {

    @Id
    @GeneratedValue
    private UUID participantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    private Party party;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @Builder
    private Participant(Party party, String name) {
        this.party = party;
        this.name = name;
        this.joinedAt = LocalDateTime.now();
    }
}
