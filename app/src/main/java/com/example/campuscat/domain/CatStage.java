package com.example.campuscat.domain;

/** 레벨에 따라 진화하는 고양이 단계. */
public enum CatStage {
    BABY(1, "아기 냥이", "이제 막 학교에 따라온 아기 고양이. 수업 시간엔 주로 잠을 자요."),
    FRESHMAN(3, "새내기 냥이", "리본을 달고 캠퍼스를 탐험하는 새내기. 강의실 위치를 외우는 중!"),
    HONOR(6, "우등생 냥이", "안경을 쓰고 맨 앞자리에 앉는 우등생. 필기가 아주 꼼꼼해요."),
    SCHOLAR(10, "장학생 냥이", "별 핀을 단 장학생. 교수님도 이름을 기억하는 고양이예요."),
    DOCTOR(15, "박사 냥이", "학사모를 쓴 박사 고양이. 이제 수업을 들으러 오는 게 아니라 하러 와요.");

    private final int minLevel;
    private final String displayName;
    private final String description;

    CatStage(int minLevel, String displayName, String description) {
        this.minLevel = minLevel;
        this.displayName = displayName;
        this.description = description;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static CatStage forLevel(int level) {
        CatStage result = BABY;
        for (CatStage stage : values()) {
            if (level >= stage.minLevel) {
                result = stage;
            }
        }
        return result;
    }

    /** 다음 진화 단계. 마지막 단계면 null. */
    public CatStage next() {
        int nextIndex = ordinal() + 1;
        CatStage[] all = values();
        return nextIndex < all.length ? all[nextIndex] : null;
    }
}
