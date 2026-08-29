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
@Table(name = "party")
public class Party {

    @Id
    @GeneratedValue
    private UUID partyId;

    /** 초대 링크에 쓰이는 6자리 코드. URL이 짧아야 공유가 쉬워서 UUID 대신 별도 코드를 쓴다. */
    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    /** 요금 안내(30명 초과 유료) 문구 계산에 쓰는 예상 인원 */
    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 파티 종료 시각. 종료되면 리포트 화면으로 넘어간다. */
    private LocalDateTime closedAt;

    @Builder
    private Party(String code, String name, int capacity) {
        this.code = code;
        this.name = name;
        this.capacity = capacity;
        this.createdAt = LocalDateTime.now();
    }

    public void close() {
        if (this.closedAt == null) {
            this.closedAt = LocalDateTime.now();
        }
    }

    public boolean isClosed() {
        return this.closedAt != null;
    }

    /** 상호 재선택(Pick) 마감 시각. 종료 후 24시간 — 화면에 노출해 B4 리텐션 트리거 근거로 쓴다. */
    public LocalDateTime getPicksDeadline() {
        return this.closedAt == null ? null : this.closedAt.plusHours(24);
    }
}
