package kr.suhsaechan.hackthebeat.party.controller;

import jakarta.validation.Valid;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.CreatePartyRequest;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.JoinRequest;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.MoodRequest;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.ParticipantResponse;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.PartyReport;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.PartyStatus;
import kr.suhsaechan.hackthebeat.party.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parties")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;

    /** 파티 생성 — 응답의 code가 초대 링크 경로가 된다 */
    @PostMapping
    public ResponseEntity<PartyStatus> create(@Valid @RequestBody CreatePartyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partyService.createParty(request));
    }

    /** 호스트 대시보드·참가자 화면이 폴링하는 현재 상태 */
    @GetMapping("/{code}")
    public PartyStatus status(@PathVariable String code) {
        return partyService.getStatus(code);
    }

    /** 초대 링크로 들어온 참가자 등록 */
    @PostMapping("/{code}/participants")
    public ResponseEntity<ParticipantResponse> join(@PathVariable String code,
                                                    @Valid @RequestBody JoinRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partyService.join(code, request));
    }

    /** 감정 버튼 클릭 기록 */
    @PostMapping("/{code}/moods")
    public PartyStatus vote(@PathVariable String code, @Valid @RequestBody MoodRequest request) {
        return partyService.vote(code, request);
    }

    /** 파티 종료 — 리포트 화면으로 넘어가는 트리거 */
    @PostMapping("/{code}/close")
    public PartyStatus close(@PathVariable String code) {
        return partyService.close(code);
    }

    /** 종료 후 '오늘의 온도 그래프' 리포트 */
    @GetMapping("/{code}/report")
    public PartyReport report(@PathVariable String code) {
        return partyService.getReport(code);
    }
}
