package kr.suhsaechan.hackthebeat.party.repository;

import java.util.List;
import java.util.UUID;
import kr.suhsaechan.hackthebeat.party.domain.Meet;
import kr.suhsaechan.hackthebeat.party.domain.Participant;
import kr.suhsaechan.hackthebeat.party.domain.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetRepository extends JpaRepository<Meet, UUID> {
    List<Meet> findByParty(Party party);

    @Query("SELECT m FROM Meet m WHERE m.party = :party AND (m.participantA = :participant OR m.participantB = :participant)")
    List<Meet> findMyMeets(@Param("party") Party party, @Param("participant") Participant participant);

    @Query("SELECT COUNT(m) > 0 FROM Meet m WHERE m.party = :party AND " +
           "((m.participantA = :p1 AND m.participantB = :p2) OR (m.participantA = :p2 AND m.participantB = :p1))")
    boolean existsMeet(@Param("party") Party party, @Param("p1") Participant p1, @Param("p2") Participant p2);
}
