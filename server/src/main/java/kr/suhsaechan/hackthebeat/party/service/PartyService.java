package kr.suhsaechan.hackthebeat.party.service;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import kr.suhsaechan.hackthebeat.party.domain.Mood;
import kr.suhsaechan.hackthebeat.party.domain.MoodVote;
import kr.suhsaechan.hackthebeat.party.domain.Participant;
import kr.suhsaechan.hackthebeat.party.domain.Party;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.Alert;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.CreatePartyRequest;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.JoinRequest;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.MoodCount;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.MoodRequest;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.ParticipantResponse;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.PartyReport;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.PartyStatus;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.TimelinePoint;
import kr.suhsaechan.hackthebeat.party.repository.MoodVoteRepository;
import kr.suhsaechan.hackthebeat.party.repository.ParticipantRepository;
import kr.suhsaechan.hackthebeat.party.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyService {

    /** 헷갈리는 문자(0/O, 1/I)를 뺀 코드 문자셋 — 초대 코드를 눈으로 옮겨 적을 수 있게 한다. */
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 개입 알림 임계치: 참가자 2명 이상이면서 '어색하다' 비율이 이 값을 넘으면 알린다. */
    private static final double AWKWARD_ALERT_RATIO = 0.5;
    private static final int MIN_PARTICIPANTS_FOR_ALERT = 2;

    /** 무료 인원. 초과분은 유료 안내 문구로만 노출한다(결제 없음). */
    private static final int FREE_CAPACITY = 30;
    private static final int PAID_PRICE = 9900;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final PartyRepository partyRepository;
    private final ParticipantRepository participantRepository;
    private final MoodVoteRepository moodVoteRepository;

    @Transactional
    public PartyStatus createParty(CreatePartyRequest request) {
        int capacity = request.capacity() == null ? FREE_CAPACITY : request.capacity();
        Party party = partyRepository.save(Party.builder()
                .code(generateUniqueCode())
                .name(request.name().trim())
                .capacity(capacity)
                .build());
        return toStatus(party);
    }

    public PartyStatus getStatus(String code) {
        return toStatus(findParty(code));
    }

    @Transactional
    public ParticipantResponse join(String code, JoinRequest request) {
        Party party = findParty(code);
        Participant participant = participantRepository.save(Participant.builder()
                .party(party)
                .name(request.name().trim())
                .build());
        return new ParticipantResponse(participant.getParticipantId().toString(), participant.getName());
    }

    @Transactional
    public PartyStatus vote(String code, MoodRequest request) {
        Party party = findParty(code);
        Participant participant = participantRepository.findById(parseId(request.participantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "참여 정보를 찾을 수 없습니다"));

        moodVoteRepository.save(MoodVote.builder()
                .party(party)
                .participant(participant)
                .mood(request.mood())
                .build());
        return toStatus(party);
    }

    @Transactional
    public PartyStatus close(String code) {
        Party party = findParty(code);
        party.close();
        return toStatus(party);
    }

    public PartyReport getReport(String code) {
        Party party = findParty(code);
        List<MoodVote> votes = moodVoteRepository.findByPartyOrderByCreatedAtAsc(party);
        long participantCount = participantRepository.countByParty(party);

        // 5분 단위로 묶어 시간대별 감정 추이를 만든다 (리포트 그래프용)
        Map<String, Map<String, Long>> buckets = new LinkedHashMap<>();
        for (MoodVote vote : votes) {
            String bucket = vote.getCreatedAt()
                    .withMinute(vote.getCreatedAt().getMinute() / 5 * 5)
                    .withSecond(0)
                    .withNano(0)
                    .format(TIME_FORMAT);
            buckets.computeIfAbsent(bucket, key -> new LinkedHashMap<>())
                    .merge(vote.getMood().name(), 1L, Long::sum);
        }
        List<TimelinePoint> timeline = buckets.entrySet().stream()
                .map(entry -> new TimelinePoint(entry.getKey(), entry.getValue()))
                .toList();

        return new PartyReport(party.getCode(), party.getName(), participantCount,
                votes.size(), countMoods(votes), timeline);
    }

    private Party findParty(String code) {
        return partyRepository.findByCode(code == null ? "" : code.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "파티를 찾을 수 없습니다"));
    }

    private UUID parseId(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "참여 정보가 올바르지 않습니다");
        }
    }

    private PartyStatus toStatus(Party party) {
        List<MoodVote> votes = moodVoteRepository.findByPartyOrderByCreatedAtAsc(party);
        long participantCount = participantRepository.countByParty(party);

        return new PartyStatus(
                party.getCode(),
                party.getName(),
                party.getCapacity(),
                participantCount,
                votes.size(),
                countMoods(votes),
                buildAlert(votes, participantCount),
                party.isClosed(),
                buildPriceNotice(party.getCapacity())
        );
    }

    /** 감정별 집계. 화면 막대가 비어 보이지 않도록 0건인 감정도 항상 포함한다. */
    private List<MoodCount> countMoods(List<MoodVote> votes) {
        Map<Mood, Long> counted = votes.stream()
                .collect(Collectors.groupingBy(MoodVote::getMood, Collectors.counting()));
        long total = votes.size();

        List<MoodCount> result = new ArrayList<>();
        for (Mood mood : Mood.values()) {
            long count = counted.getOrDefault(mood, 0L);
            int percent = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
            result.add(new MoodCount(mood.name(), mood.getLabel(), count, percent));
        }
        return result;
    }

    /**
     * '어색하다'가 절반을 넘으면 호스트에게 개입 시점을 알린다.
     * 표본이 1명뿐일 때 울리면 오탐이라 최소 인원 조건을 둔다.
     */
    private Alert buildAlert(List<MoodVote> votes, long participantCount) {
        if (votes.isEmpty() || participantCount < MIN_PARTICIPANTS_FOR_ALERT) {
            return new Alert(false, null);
        }
        long awkward = votes.stream().filter(vote -> vote.getMood() == Mood.AWKWARD).count();
        if ((double) awkward / votes.size() < AWKWARD_ALERT_RATIO) {
            return new Alert(false, null);
        }
        return new Alert(true, String.format(
                "\"어색하다\"가 %d개 중 %d개입니다 — 게임 하나 돌릴 시점입니다", votes.size(), awkward));
    }

    private String buildPriceNotice(int capacity) {
        if (capacity <= FREE_CAPACITY) {
            return String.format("%d명까지 무료", FREE_CAPACITY);
        }
        return String.format("%d명까지 무료 / 초과 시 %,d원", FREE_CAPACITY, PAID_PRICE);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder builder = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                builder.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = builder.toString();
            if (!partyRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("초대 코드 생성에 실패했습니다");
    }
}
