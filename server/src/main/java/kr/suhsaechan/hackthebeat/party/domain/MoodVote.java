package kr.suhsaechan.hackthebeat.party.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참가자가 누른 감정 기록. 같은 사람이 여러 번 눌러도 전부 남긴다
 * (파티 진행에 따른 분위기 변화를 리포트에서 보여주기 위함).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mood_vote")
public class MoodVote {

    @Id
    @GeneratedValue
    private UUID moodVoteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id")
    private Participant participant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Mood mood;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private MoodVote(Party party, Participant participant, Mood mood) {
        this.party = party;
        this.participant = participant;
        this.mood = mood;
        this.createdAt = LocalDateTime.now();
    }
}
