package kr.suhsaechan.hackthebeat.party.service;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.*;
import kr.suhsaechan.hackthebeat.party.domain.Badge;
import kr.suhsaechan.hackthebeat.party.domain.Meet;
import kr.suhsaechan.hackthebeat.party.domain.Participant;
import kr.suhsaechan.hackthebeat.party.domain.Party;
import kr.suhsaechan.hackthebeat.party.domain.Pick;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.*;
import kr.suhsaechan.hackthebeat.party.repository.MeetRepository;
import kr.suhsaechan.hackthebeat.party.repository.ParticipantRepository;
import kr.suhsaechan.hackthebeat.party.repository.PartyRepository;
import kr.suhsaechan.hackthebeat.party.repository.PickRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyService {

    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int PARTY_CODE_LENGTH = 6;
    private static final int TAG_CODE_LENGTH = 4;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int FREE_CAPACITY = 20;
    private static final int PAID_PRICE = 9900;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    private static final List<String> VALID_CHARACTERS = List.of(
            "FOX", "FROG", "PANDA", "CHICK", "OCTOPUS", "LION", "RABBIT", "KOALA"
    );
    private static final Set<String> VALID_CHARACTER_SET = Set.copyOf(VALID_CHARACTERS);

    private static final Set<String> VALID_INTEREST_SET = Set.of(
            "게임", "러닝", "영화", "음악", "여행", "요리", "독서", "그림", "축구", "반려동물", "카페", "개발"
    );

    private final PartyRepository partyRepository;
    private final ParticipantRepository participantRepository;
    private final MeetRepository meetRepository;
    private final PickRepository pickRepository;

    @Transactional
    public PassportResponse createParty(CreatePartyRequest request) {
        int capacity = request.capacity() == null ? FREE_CAPACITY : request.capacity();
        Party party = partyRepository.save(Party.builder()
                .code(generateUniquePartyCode())
                .name(request.name().trim())
                .capacity(capacity)
                .build());

        String hostName = (request.hostName() == null || request.hostName().isBlank())
                ? "호스트" : request.hostName().trim();

        String hostCharacter = normalizeCharacter(request.hostCharacter());
        String hostInterests = normalizeInterests(request.hostInterests());

        Participant host = participantRepository.save(Participant.builder()
                .party(party)
                .name(hostName)
                .tagCode(generateUniqueTagCode(party))
                .isHost(true)
                .characterKey(hostCharacter)
                .interests(hostInterests)
                .build());

        return buildPassport(party, host);
    }

    public PartyStatus getStatus(String code) {
        Party party = findParty(code);
        long participantCount = participantRepository.countByParty(party);
        long meetCount = meetRepository.findByParty(party).size();

        return new PartyStatus(
                party.getCode(),
                party.getName(),
                party.getCapacity(),
                participantCount,
                meetCount,
                party.isClosed(),
                buildPriceNotice(party.getCapacity())
        );
    }

    @Transactional
    public PassportResponse join(String code, JoinRequest request) {
        Party party = findParty(code);
        if (party.isClosed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 종료된 파티입니다");
        }

        // 직전 참여자를 미션 상대로 지정
        Optional<Participant> lastParticipantOpt = participantRepository.findTopByPartyOrderByJoinedAtDesc(party);

        String character = normalizeCharacter(request.character());
        String interests = normalizeInterests(request.interests());

        Participant participant = participantRepository.save(Participant.builder()
                .party(party)
                .name(request.name().trim())
                .tagCode(generateUniqueTagCode(party))
                .missionTargetParticipantId(lastParticipantOpt.map(Participant::getParticipantId).orElse(null))
                .isHost(false)
                .characterKey(character)
                .interests(interests)
                .build());

        // 첫 번째 호스트에게 두 번째 참여자를 미션 상대로 연결
        if (lastParticipantOpt.isPresent()) {
            Participant lastParticipant = lastParticipantOpt.get();
            if (lastParticipant.getMissionTargetParticipantId() == null) {
                lastParticipant.updateMissionTarget(participant.getParticipantId());
            }
        }

        // 초대 링크의 fromTagCode가 있으면 즉시 상호 태그(Meet) 생성
        if (request.fromTagCode() != null && !request.fromTagCode().isBlank()) {
            participantRepository.findByPartyAndTagCode(party, request.fromTagCode().trim().toUpperCase())
                    .ifPresent(inviter -> {
                        if (!inviter.getParticipantId().equals(participant.getParticipantId())) {
                            createMeetIfNotExists(party, inviter, participant);
                        }
                    });
        }

        return buildPassport(party, participant);
    }

    @Transactional
    public PassportResponse tagPerson(String code, TagRequest request) {
        Party party = findParty(code);
        Participant me = findParticipant(request.participantId());
        Participant target = participantRepository.findByPartyAndTagCode(party, request.targetTagCode().trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 코드의 참가자를 찾을 수 없습니다"));

        if (me.getParticipantId().equals(target.getParticipantId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자신을 태그할 수 없습니다");
        }

        createMeetIfNotExists(party, me, target);
        return buildPassport(party, me);
    }

    public PassportResponse getPassport(String code, String participantId) {
        Party party = findParty(code);
        Participant me = findParticipant(participantId);
        return buildPassport(party, me);
    }

    @Transactional
    public PartyStatus close(String code, String participantId) {
        Party party = findParty(code);
        if (participantId != null && !participantId.isBlank()) {
            Participant participant = findParticipant(participantId);
            if (!participant.getParty().getPartyId().equals(party.getPartyId()) || !participant.isHost()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "호스트만 파티를 종료할 수 있습니다");
            }
        }
        party.close();
        return getStatus(code);
    }

    @Transactional
    public PartyStatus close(String code) {
        return close(code, null);
    }

    @Transactional
    public MatchResponse submitPicks(String code, SubmitPicksRequest request) {
        Party party = findParty(code);
        Participant me = findParticipant(request.participantId());

        if (request.picks() != null && !request.picks().isEmpty()) {
            for (PickItem item : request.picks()) {
                if (item.targetTagCode() == null || item.targetTagCode().isBlank()) {
                    continue;
                }
                Optional<Participant> targetOpt = participantRepository.findByPartyAndTagCode(party, item.targetTagCode().trim().toUpperCase());
                if (targetOpt.isPresent()) {
                    Participant target = targetOpt.get();
                    if (!me.getParticipantId().equals(target.getParticipantId())) {
                        int level = item.level() == null ? 2 : Math.max(1, Math.min(3, item.level()));
                        if (!pickRepository.existsByPartyAndFromParticipantAndToParticipant(party, me, target)) {
                            pickRepository.save(Pick.builder()
                                    .party(party)
                                    .fromParticipant(me)
                                    .toParticipant(target)
                                    .pickLevel(level)
                                    .build());
                        }
                    }
                }
            }
        } else if (request.targetParticipantIds() != null) {
            for (String targetIdStr : request.targetParticipantIds()) {
                try {
                    Participant target = findParticipant(targetIdStr);
                    if (!me.getParticipantId().equals(target.getParticipantId())) {
                        if (!pickRepository.existsByPartyAndFromParticipantAndToParticipant(party, me, target)) {
                            pickRepository.save(Pick.builder()
                                    .party(party)
                                    .fromParticipant(me)
                                    .toParticipant(target)
                                    .pickLevel(2)
                                    .build());
                        }
                    }
                } catch (ResponseStatusException ignored) {
                }
            }
        }

        return getMatches(code, request.participantId());
    }

    public MatchResponse getMatches(String code, String participantId) {
        Party party = findParty(code);
        Participant me = findParticipant(participantId);

        List<Participant> mutualMatches = pickRepository.findMutualMatches(party, me);
        List<Pick> partyPicks = pickRepository.findByParty(party);
        Map<String, Integer> pickLevelMap = new HashMap<>();
        for (Pick pick : partyPicks) {
            String key = pick.getFromParticipant().getParticipantId() + "->" + pick.getToParticipant().getParticipantId();
            pickLevelMap.put(key, pick.getPickLevel());
        }

        List<MetPersonDto> matchDtos = mutualMatches.stream()
                .map(p -> new MetPersonDto(
                        p.getName(),
                        p.getTagCode(),
                        null,
                        p.getCharacterKey(),
                        parseInterests(p.getInterests()),
                        pickLevelMap.get(me.getParticipantId() + "->" + p.getParticipantId()),
                        pickLevelMap.get(p.getParticipantId() + "->" + me.getParticipantId())
                ))
                .toList();

        List<MetPersonDto> allMet = getMyMetPersons(party, me);
        String picksDeadline = party.getPicksDeadline() == null
                ? null : party.getPicksDeadline().format(DEADLINE_FORMAT);

        return new MatchResponse(
                me.getParticipantId().toString(),
                me.getName(),
                matchDtos.size(),
                matchDtos,
                allMet,
                !matchDtos.isEmpty(),
                picksDeadline
        );
    }

    private String normalizeCharacter(String character) {
        if (character == null || character.isBlank()) {
            return getRandomCharacter();
        }
        String upper = character.trim().toUpperCase();
        if (VALID_CHARACTER_SET.contains(upper)) {
            return upper;
        }
        return getRandomCharacter();
    }

    private String getRandomCharacter() {
        return VALID_CHARACTERS.get(RANDOM.nextInt(VALID_CHARACTERS.size()));
    }

    private String normalizeInterests(List<String> interests) {
        if (interests == null || interests.isEmpty()) {
            return null;
        }
        List<String> validInterests = interests.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(VALID_INTEREST_SET::contains)
                .distinct()
                .limit(3)
                .toList();

        return validInterests.isEmpty() ? null : String.join(",", validInterests);
    }

    private void createMeetIfNotExists(Party party, Participant a, Participant b) {
        if (!meetRepository.existsMeet(party, a, b)) {
            meetRepository.save(Meet.builder()
                    .party(party)
                    .participantA(a)
                    .participantB(b)
                    .build());
        }
    }

    private PassportResponse buildPassport(Party party, Participant me) {
        List<Meet> myMeets = meetRepository.findMyMeets(party, me);
        long totalParticipants = participantRepository.countByParty(party);
        List<MetPersonDto> metPersons = new ArrayList<>();

        boolean missionCleared = false;
        String missionTargetName = null;
        String missionTargetCharacter = null;
        List<String> missionTargetInterests = List.of();

        if (me.getMissionTargetParticipantId() != null) {
            Optional<Participant> targetOpt = participantRepository.findById(me.getMissionTargetParticipantId());
            if (targetOpt.isPresent()) {
                Participant target = targetOpt.get();
                missionTargetName = target.getName();
                missionTargetCharacter = target.getCharacterKey();
                missionTargetInterests = parseInterests(target.getInterests());
            }
        }

        for (Meet m : myMeets) {
            Participant other = m.getParticipantA().getParticipantId().equals(me.getParticipantId())
                    ? m.getParticipantB() : m.getParticipantA();
            metPersons.add(new MetPersonDto(
                    other.getName(),
                    other.getTagCode(),
                    m.getCreatedAt().format(TIME_FORMAT),
                    other.getCharacterKey(),
                    parseInterests(other.getInterests()),
                    null,
                    null
            ));
            if (me.getMissionTargetParticipantId() != null &&
                other.getParticipantId().equals(me.getMissionTargetParticipantId())) {
                missionCleared = true;
            }
        }

        int metCount = metPersons.size();
        int progressGoal = (int) Math.max(1, totalParticipants - 1);
        int progressPercent = Math.min(100, (int) Math.round((double) metCount / progressGoal * 100));

        // 증표 계산
        boolean hasFirstMeet = metCount >= 1;
        boolean hasIceBreaker = metCount >= 3;
        boolean hasPartyPeople = totalParticipants >= 2 && metCount >= (totalParticipants + 1) / 2;
        boolean hasPartyMaster = totalParticipants >= 2 && metCount >= (totalParticipants - 1);
        boolean hasMissionClear = missionCleared;
        boolean hasReunion = party.isClosed() && !pickRepository.findMutualMatches(party, me).isEmpty();

        List<BadgeDto> badges = List.of(
                new BadgeDto(Badge.FIRST_MEET.name(), Badge.FIRST_MEET.getTitle(), Badge.FIRST_MEET.getDescription(), hasFirstMeet),
                new BadgeDto(Badge.ICE_BREAKER.name(), Badge.ICE_BREAKER.getTitle(), Badge.ICE_BREAKER.getDescription(), hasIceBreaker),
                new BadgeDto(Badge.PARTY_PEOPLE.name(), Badge.PARTY_PEOPLE.getTitle(), Badge.PARTY_PEOPLE.getDescription(), hasPartyPeople),
                new BadgeDto(Badge.PARTY_MASTER.name(), Badge.PARTY_MASTER.getTitle(), Badge.PARTY_MASTER.getDescription(), hasPartyMaster),
                new BadgeDto(Badge.MISSION_CLEAR.name(), Badge.MISSION_CLEAR.getTitle(), Badge.MISSION_CLEAR.getDescription(), hasMissionClear),
                new BadgeDto(Badge.REUNION.name(), Badge.REUNION.getTitle(), Badge.REUNION.getDescription(), hasReunion)
        );

        return new PassportResponse(
                party.getCode(),
                party.getName(),
                me.getParticipantId().toString(),
                me.getName(),
                me.getTagCode(),
                me.isHost(),
                party.isClosed(),
                metCount,
                totalParticipants,
                progressPercent,
                badges,
                metPersons,
                missionTargetName,
                missionCleared,
                buildPriceNotice(party.getCapacity()),
                me.getCharacterKey(),
                parseInterests(me.getInterests()),
                calculateGrowthStage(metCount),
                missionTargetCharacter,
                missionTargetInterests
        );
    }

    private List<MetPersonDto> getMyMetPersons(Party party, Participant me) {
        List<Meet> myMeets = meetRepository.findMyMeets(party, me);
        List<MetPersonDto> metPersons = new ArrayList<>();
        for (Meet m : myMeets) {
            Participant other = m.getParticipantA().getParticipantId().equals(me.getParticipantId())
                    ? m.getParticipantB() : m.getParticipantA();
            metPersons.add(new MetPersonDto(
                    other.getName(),
                    other.getTagCode(),
                    m.getCreatedAt().format(TIME_FORMAT),
                    other.getCharacterKey(),
                    parseInterests(other.getInterests()),
                    null,
                    null
            ));
        }
        return metPersons;
    }

    private List<String> parseInterests(String interests) {
        if (interests == null || interests.isBlank()) {
            return List.of();
        }
        return Arrays.stream(interests.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private int calculateGrowthStage(int metCount) {
        if (metCount >= 3) return 3;
        if (metCount >= 1) return 2;
        return 1;
    }

    private Party findParty(String code) {
        return partyRepository.findByCode(code == null ? "" : code.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "파티를 찾을 수 없습니다"));
    }

    private Participant findParticipant(String idStr) {
        try {
            UUID id = UUID.fromString(idStr);
            return participantRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "참가자 정보를 찾을 수 없습니다"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 참가자 식별자입니다");
        }
    }

    private String buildPriceNotice(int capacity) {
        if (capacity <= FREE_CAPACITY) {
            return String.format("%d명까지 무료", FREE_CAPACITY);
        }
        return String.format("%d명까지 무료 / 초과 시 %,d원", FREE_CAPACITY, PAID_PRICE);
    }

    private String generateUniquePartyCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder builder = new StringBuilder(PARTY_CODE_LENGTH);
            for (int i = 0; i < PARTY_CODE_LENGTH; i++) {
                builder.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = builder.toString();
            if (!partyRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("파티 코드 생성에 실패했습니다");
    }

    private String generateUniqueTagCode(Party party) {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder builder = new StringBuilder(TAG_CODE_LENGTH);
            for (int i = 0; i < TAG_CODE_LENGTH; i++) {
                builder.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String tagCode = builder.toString();
            if (!participantRepository.existsByPartyAndTagCode(party, tagCode)) {
                return tagCode;
            }
        }
        throw new IllegalStateException("태그 코드 생성에 실패했습니다");
    }
}
