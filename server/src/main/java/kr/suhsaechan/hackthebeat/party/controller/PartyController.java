package kr.suhsaechan.hackthebeat.party.controller;

import jakarta.validation.Valid;
import kr.suhsaechan.hackthebeat.party.dto.PartyDto.*;
import kr.suhsaechan.hackthebeat.party.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parties")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PassportResponse create(@Valid @RequestBody CreatePartyRequest request) {
        return partyService.createParty(request);
    }

    @GetMapping("/{code}")
    public PartyStatus getStatus(@PathVariable String code) {
        return partyService.getStatus(code);
    }

    @PostMapping("/{code}/join")
    @ResponseStatus(HttpStatus.CREATED)
    public PassportResponse join(@PathVariable String code, @Valid @RequestBody JoinRequest request) {
        return partyService.join(code, request);
    }

    @GetMapping("/{code}/passport/{participantId}")
    public PassportResponse getPassport(@PathVariable String code, @PathVariable String participantId) {
        return partyService.getPassport(code, participantId);
    }

    @PostMapping("/{code}/tag")
    public PassportResponse tag(@PathVariable String code, @Valid @RequestBody TagRequest request) {
        return partyService.tagPerson(code, request);
    }

    @PostMapping("/{code}/close")
    public PartyStatus close(
            @PathVariable String code,
            @RequestBody(required = false) CloseRequest request,
            @RequestParam(required = false) String participantId
    ) {
        String resolvedParticipantId = (request != null && request.participantId() != null)
                ? request.participantId() : participantId;
        return partyService.close(code, resolvedParticipantId);
    }

    @PostMapping("/{code}/picks")
    public MatchResponse submitPicks(@PathVariable String code, @Valid @RequestBody SubmitPicksRequest request) {
        return partyService.submitPicks(code, request);
    }

    @GetMapping("/{code}/matches/{participantId}")
    public MatchResponse getMatches(@PathVariable String code, @PathVariable String participantId) {
        return partyService.getMatches(code, participantId);
    }
}
