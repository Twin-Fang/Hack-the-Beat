package kr.suhsaechan.hackthebeat.party.domain;

/**
 * 참가자가 누르는 감정 버튼 4종. label은 화면·심사 시나리오에 쓰이는 문구와 일치시킨다.
 */
public enum Mood {
    FUN("재밌다"),
    AWKWARD("어색하다"),
    HUNGRY("배고프다"),
    QUIET("조용했으면");

    private final String label;

    Mood(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
