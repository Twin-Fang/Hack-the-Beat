package kr.suhsaechan.hackthebeat.party.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Badge {
    FIRST_MEET("첫 만남", "첫 번째 대화 상대를 만났어요!"),
    ICE_BREAKER("아이스브레이커", "3명과 인사를 나누며 분위기를 풀었어요!"),
    PARTY_PEOPLE("파티 피플", "파티 참가자의 절반 이상과 대화했어요!"),
    PARTY_MASTER("파티 마스터", "파티의 모든 사람과 인사를 마쳤어요!"),
    MISSION_CLEAR("미션 완료", "나만의 특별 미션 상대를 찾아 대화했어요!"),
    REUNION("재회", "서로 다시 만나고 싶은 사람과 매칭되었어요!");

    private final String title;
    private final String description;
}
