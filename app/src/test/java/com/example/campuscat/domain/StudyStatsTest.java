package com.example.campuscat.domain;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class StudyStatsTest {

    private static final TimeZone SEOUL = TimeZone.getTimeZone("Asia/Seoul");
    private static final long MINUTE = 60_000L;

    private static long at(int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(SEOUL);
        calendar.clear();
        calendar.set(2026, month - 1, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }

    private static StudySession session(long start, long minutes) {
        return new StudySession(start, start + minutes * MINUTE, "");
    }

    @Test
    public void sessionDurationAndXp() {
        StudySession s = new StudySession(at(10, 27, 9, 0), at(10, 27, 10, 15) + 59_000L, "  자료구조 ");
        assertEquals(75, s.getDurationMinutes());
        assertEquals(75, s.getXp());
        assertEquals("자료구조", s.getSubject());
    }

    @Test
    public void endBeforeStartIsClampedToZero() {
        StudySession s = new StudySession(at(10, 27, 9, 0), at(10, 27, 8, 0), null);
        assertEquals(0, s.getDurationMinutes());
        assertEquals("", s.getSubject());
    }

    @Test
    public void sessionAcrossMidnightIsSplitBetweenDays() {
        List<StudySession> sessions = Collections.singletonList(session(at(10, 26, 23, 30), 90));
        assertEquals(30, StudyStats.minutesOnDay(sessions, at(10, 26, 12, 0), SEOUL));
        assertEquals(60, StudyStats.minutesOnDay(sessions, at(10, 27, 12, 0), SEOUL));
        assertEquals(90, StudyStats.totalMinutes(sessions));
    }

    @Test
    public void dailyMinutesEndsWithToday() {
        List<StudySession> sessions = Arrays.asList(
                session(at(10, 27, 9, 0), 50),
                session(at(10, 27, 13, 0), 70),
                session(at(10, 25, 10, 0), 30),
                session(at(10, 18, 10, 0), 999));
        long[] daily = StudyStats.dailyMinutes(sessions, at(10, 27, 20, 0), 7, SEOUL);
        assertArrayEquals(new long[]{0, 0, 0, 0, 30, 0, 120}, daily);
    }

    @Test
    public void streakCountsConsecutiveDays() {
        List<StudySession> sessions = new ArrayList<>();
        sessions.add(session(at(10, 27, 9, 0), 10));
        sessions.add(session(at(10, 26, 9, 0), 10));
        sessions.add(session(at(10, 25, 9, 0), 10));
        sessions.add(session(at(10, 23, 9, 0), 10));
        assertEquals(3, StudyStats.streakDays(sessions, at(10, 27, 22, 0), SEOUL));
    }

    @Test
    public void streakStillCountsFromYesterdayBeforeStudyingToday() {
        List<StudySession> sessions = Arrays.asList(
                session(at(10, 26, 9, 0), 10),
                session(at(10, 25, 9, 0), 10));
        assertEquals(2, StudyStats.streakDays(sessions, at(10, 27, 8, 0), SEOUL));
        assertEquals(0, StudyStats.streakDays(sessions, at(10, 28, 8, 0), SEOUL));
    }

    @Test
    public void emptyHistory() {
        List<StudySession> none = Collections.emptyList();
        assertEquals(0, StudyStats.streakDays(none, at(10, 27, 8, 0), SEOUL));
        assertEquals(0, StudyStats.totalXp(none));
    }

    @Test
    public void totalXpSumsWholeMinutesPerSession() {
        List<StudySession> sessions = Arrays.asList(
                new StudySession(0, 90_000L, ""),
                new StudySession(0, 90_000L, ""));
        assertEquals(2, StudyStats.totalXp(sessions));
        assertEquals(3, StudyStats.totalMinutes(sessions));
    }
}
