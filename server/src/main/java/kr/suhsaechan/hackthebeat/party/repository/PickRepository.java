package kr.suhsaechan.hackthebeat.party.repository;

import java.util.List;
import java.util.UUID;
import kr.suhsaechan.hackthebeat.party.domain.Participant;
import kr.suhsaechan.hackthebeat.party.domain.Party;
import kr.suhsaechan.hackthebeat.party.domain.Pick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PickRepository extends JpaRepository<Pick, UUID> {
    List<Pick> findByPartyAndFromParticipant(Party party, Participant fromParticipant);
    List<Pick> findByParty(Party party);

    @Query("SELECT p1.toParticipant FROM Pick p1 " +
           "WHERE p1.party = :party AND p1.fromParticipant = :me " +
           "AND EXISTS (SELECT 1 FROM Pick p2 WHERE p2.party = :party AND p2.fromParticipant = p1.toParticipant AND p2.toParticipant = :me)")
    List<Participant> findMutualMatches(@Param("party") Party party, @Param("me") Participant me);

    boolean existsByPartyAndFromParticipantAndToParticipant(Party party, Participant from, Participant to);
}
