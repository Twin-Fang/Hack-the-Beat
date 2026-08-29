package kr.suhsaechan.hackthebeat.party.repository;

import java.util.Optional;
import java.util.UUID;
import kr.suhsaechan.hackthebeat.party.domain.Party;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyRepository extends JpaRepository<Party, UUID> {

    Optional<Party> findByCode(String code);

    boolean existsByCode(String code);
}
