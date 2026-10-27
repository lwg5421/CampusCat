package com.example.campuscat.domain;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * 수업 기록 통계 계산. 자정을 넘긴 수업은 날짜별로 나누어 집계한다.
 */
public final class StudyStats {

    private static final long MILLIS_PER_MINUTE = 60_000L;

    private StudyStats() {
    }

    public static long startOfDay(long millis, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /** {@code dayStart} 에서 {@code days} 일 이동한 날의 시작 시각. */
    public static long addDays(long dayStart, int days, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(dayStart);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return startOfDay(calendar.getTimeInMillis(), timeZone);
    }

    /** [rangeStart, rangeEnd) 구간과 겹치는 수업 시간의 합(분). */
    public static long minutesInRange(List<StudySession> sessions, long rangeStart, long rangeEnd) {
        long totalMillis = 0;
        for (StudySession session : sessions) {
            long start = Math.max(session.getStartMillis(), rangeStart);
            long end = Math.min(session.getEndMillis(), rangeEnd);
            if (end > start) {
                totalMillis += end - start;
            }
        }
        return totalMillis / MILLIS_PER_MINUTE;
    }

    public static long minutesOnDay(List<StudySession> sessions, long anyMillisOfDay, TimeZone timeZone) {
        long dayStart = startOfDay(anyMillisOfDay, timeZone);
        return minutesInRange(sessions, dayStart, addDays(dayStart, 1, timeZone));
    }

    /**
     * 오늘을 포함한 최근 {@code days} 일의 날짜별 수업 시간(분).
     * 배열의 마지막 원소가 오늘이다.
     */
    public static long[] dailyMinutes(List<StudySession> sessions, long now, int days, TimeZone timeZone) {
        long[] result = new long[days];
        long today = startOfDay(now, timeZone);
        for (int i = 0; i < days; i++) {
            long dayStart = addDays(today, i - (days - 1), timeZone);
            result[i] = minutesInRange(sessions, dayStart, addDays(dayStart, 1, timeZone));
        }
        return result;
    }

    public static long totalMinutes(List<StudySession> sessions) {
        long totalMillis = 0;
        for (StudySession session : sessions) {
            totalMillis += session.getDurationMillis();
        }
        return totalMillis / MILLIS_PER_MINUTE;
    }

    public static int totalXp(List<StudySession> sessions) {
        long xp = 0;
        for (StudySession session : sessions) {
            xp += session.getXp();
        }
        return (int) Math.min(Integer.MAX_VALUE, xp);
    }

    /**
     * 연속으로 1분 이상 공부한 날 수. 오늘 아직 기록이 없으면 어제부터 센다
     * (오늘 공부하기 전에 연속 기록이 0으로 보이지 않도록).
     */
    public static int streakDays(List<StudySession> sessions, long now, TimeZone timeZone) {
        long day = startOfDay(now, timeZone);
        if (minutesInRange(sessions, day, addDays(day, 1, timeZone)) == 0) {
            day = addDays(day, -1, timeZone);
        }
        // 기록은 유한하므로 가장 오래된 기록보다 앞선 날에서 반드시 멈춘다.
        int streak = 0;
        while (minutesInRange(sessions, day, addDays(day, 1, timeZone)) > 0) {
            streak++;
            day = addDays(day, -1, timeZone);
        }
        return streak;
    }
}
