package com.example.campuscat.domain;

/** 한 번의 수업(공부) 기록. 시작 시각을 기록의 고유 id 로 쓴다. */
public final class StudySession {

    private static final long MILLIS_PER_MINUTE = 60_000L;

    private final long startMillis;
    private final long endMillis;
    private final String subject;

    public StudySession(long startMillis, long endMillis, String subject) {
        this.startMillis = startMillis;
        this.endMillis = Math.max(startMillis, endMillis);
        this.subject = subject == null ? "" : subject.trim();
    }

    public long getStartMillis() {
        return startMillis;
    }

    public long getEndMillis() {
        return endMillis;
    }

    public String getSubject() {
        return subject;
    }

    public boolean hasSubject() {
        return !subject.isEmpty();
    }

    public long getDurationMillis() {
        return endMillis - startMillis;
    }

    public long getDurationMinutes() {
        return getDurationMillis() / MILLIS_PER_MINUTE;
    }

    public int getXp() {
        return LevelSystem.xpForMinutes(getDurationMinutes());
    }
}
