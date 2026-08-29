package kr.suhsaechan.hackthebeat.party.repository;

import java.util.List;
import java.util.UUID;
import kr.suhsaechan.hackthebeat.party.domain.MoodVote;
import kr.suhsaechan.hackthebeat.party.domain.Party;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoodVoteRepository extends JpaRepository<MoodVote, UUID> {

    List<MoodVote> findByPartyOrderByCreatedAtAsc(Party party);
}
