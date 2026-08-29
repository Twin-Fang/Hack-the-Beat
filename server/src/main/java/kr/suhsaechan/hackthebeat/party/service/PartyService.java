package kr.suhsaechan.hackthebeat.party.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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
        // 화면의 결제 확인 단계는 UI 게이트일 뿐이라, API를 직접 호출하면 우회될 수 있다 —
        // 서버에서도 동일한 조건을 강제해 요금제가 실제로 걸리게 한다
        if (capacity > FREE_CAPACITY && !Boolean.TRUE.equals(request.paid())) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "20명 초과 파티는 결제 확인이 필요합니다");
        }
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

        Optional<Participant> lastParticipantOpt = participantRepository.findTopByPartyOrderByJoinedAtDesc(party);

        String character = normalizeCharacter(request.character());
        String interests = normalizeInterests(request.interests());

        Participant participant = participantRepository.save(Participant.builder()
                .party(party)
                .name(request.name().trim())
                .tagCode(generateUniqueTagCode(party))
                .missionTargetParticipantId(pickMissionTarget(party, request.fromTagCode()))
                .isHost(false)
                .characterKey(character)
                .interests(interests)
                .build());

        // 미션 상대가 아직 없는 기존 참가자에게도 상대를 채워준다.
        // 단 방금 들어온 사람과 초대 관계로 묶인 참가자는 제외한다 — 이미 만난 사이라 미션이 곧바로 완료된다
        if (lastParticipantOpt.isPresent()) {
            Participant lastParticipant = lastParticipantOpt.get();
            boolean invitedByLast = request.fromTagCode() != null
                    && lastParticipant.getTagCode().equalsIgnoreCase(request.fromTagCode().trim());
            if (lastParticipant.getMissionTargetParticipantId() == null && !invitedByLast) {
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
        Participant me = findParticipantInParty(party, request.participantId());
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
        Participant me = findParticipantInParty(party, participantId);
        return buildPassport(party, me);
    }

    @Transactional
    public PassportResponse updateInstagram(String code, String participantId, UpdateInstagramRequest request) {
        Party party = findParty(code);
        Participant me = findParticipantInParty(party, participantId);

        String normalized = normalizeInstagramId(request.instagramId());
        // 실제로 값을 등록/변경할 때만 동의가 필요하다 — 지우는 요청(null/빈 문자열)은 동의 여부와 무관하다
        if (normalized != null && !request.consent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "개인정보 수집 동의가 필요합니다");
        }

        me.updateInstagramId(normalized);
        return buildPassport(party, me);
    }

    private String normalizeInstagramId(String instagramId) {
        if (instagramId == null) {
            return null;
        }
        String trimmed = instagramId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("@")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Transactional
    public PartyStatus close(String code, String participantId) {
        Party party = findParty(code);
        // participantId 미제공 시 검증을 건너뛰면 코드만 알아도 누구나 종료 가능해지므로 기본 거부한다
        if (participantId == null || participantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "호스트만 파티를 종료할 수 있습니다");
        }
        Participant participant = findParticipant(participantId);
        if (!participant.getParty().getPartyId().equals(party.getPartyId()) || !participant.isHost()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "호스트만 파티를 종료할 수 있습니다");
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
        Participant me = findParticipantInParty(party, request.participantId());
        // 마감이 지나면 더 받지 않는다 — 화면에 안내한 24시간이 표시용에 그치지 않게 한다
        if (party.getPicksDeadline() != null
                && LocalDateTime.now().isAfter(party.getPicksDeadline())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "상호 선택 마감이 지났습니다");
        }

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
                    Participant target = findParticipantInParty(party, targetIdStr);
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
        Participant me = findParticipantInParty(party, participantId);

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
                        pickLevelMap.get(p.getParticipantId() + "->" + me.getParticipantId()),
                        p.getInstagramId()
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
                    null,
                    other.getInstagramId()
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
                missionTargetInterests,
                me.getInstagramId()
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
                    null,
                    other.getInstagramId()
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

    /** 참가자가 그 파티 소속인지까지 확인한다 — 다른 파티의 식별자로 넘어오는 요청을 막는다 */
    private Participant findParticipantInParty(Party party, String idStr) {
        Participant participant = findParticipant(idStr);
        if (participant.getParty() == null
                || !participant.getParty().getPartyId().equals(party.getPartyId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이 파티의 참가자가 아닙니다");
        }
        return participant;
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

    /**
     * 미션 상대를 고른다. 초대자는 참여 즉시 상호 태그되어 미션이 곧바로 완료되므로 후보에서 뺀다.
     * 후보가 없으면 null을 돌려주고, 화면에서는 미션 카드 대신 안내가 나간다.
     */
    private UUID pickMissionTarget(Party party, String fromTagCode) {
        String inviterCode = fromTagCode == null ? null : fromTagCode.trim().toUpperCase();
        List<Participant> candidates = participantRepository.findByPartyOrderByJoinedAtAsc(party).stream()
                .filter(p -> inviterCode == null || !p.getTagCode().equalsIgnoreCase(inviterCode))
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())).getParticipantId();
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
