package kr.suhsaechan.hackthebeat.party.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.suhsaechan.hackthebeat.party.domain.Participant;
import kr.suhsaechan.hackthebeat.party.domain.Party;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
    Optional<Participant> findByPartyAndTagCode(Party party, String tagCode);
    List<Participant> findByPartyOrderByJoinedAtAsc(Party party);
    Optional<Participant> findTopByPartyOrderByJoinedAtDesc(Party party);
    long countByParty(Party party);
    boolean existsByPartyAndTagCode(Party party, String tagCode);
}
