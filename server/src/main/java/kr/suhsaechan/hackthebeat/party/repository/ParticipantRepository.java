package kr.suhsaechan.hackthebeat.party.repository;

import java.util.List;
import java.util.UUID;
import kr.suhsaechan.hackthebeat.party.domain.Participant;
import kr.suhsaechan.hackthebeat.party.domain.Party;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    List<Participant> findByPartyOrderByJoinedAtAsc(Party party);

    long countByParty(Party party);
}
