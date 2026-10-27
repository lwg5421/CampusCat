package com.example.campuscat.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class ReminderTimeTest {

    private static final TimeZone SEOUL = TimeZone.getTimeZone("Asia/Seoul");

    private static long at(int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(SEOUL);
        calendar.clear();
        calendar.set(2026, Calendar.OCTOBER, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }

    @Test
    public void laterTodayIfNotPassedYet() {
        assertEquals(at(27, 9, 0), ReminderTime.nextTriggerMillis(at(27, 8, 30), 9, 0, SEOUL));
    }

    @Test
    public void tomorrowIfAlreadyPassedOrExactlyNow() {
        assertEquals(at(28, 9, 0), ReminderTime.nextTriggerMillis(at(27, 9, 1), 9, 0, SEOUL));
        assertEquals(at(28, 9, 0), ReminderTime.nextTriggerMillis(at(27, 9, 0), 9, 0, SEOUL));
    }

    @Test
    public void rollsOverMonthEnd() {
        Calendar expected = Calendar.getInstance(SEOUL);
        expected.clear();
        expected.set(2026, Calendar.NOVEMBER, 1, 7, 30, 0);
        assertEquals(expected.getTimeInMillis(), ReminderTime.nextTriggerMillis(at(31, 23, 0), 7, 30, SEOUL));
    }

    @Test
    public void format() {
        assertEquals("09:05", ReminderTime.format(9, 5));
        assertEquals("21:30", ReminderTime.format(21, 30));
    }
}
