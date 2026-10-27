package com.example.campuscat.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimeFormatterTest {

    @Test
    public void formatDuration() {
        assertEquals("0분", TimeFormatter.formatDuration(0));
        assertEquals("45분", TimeFormatter.formatDuration(45));
        assertEquals("1시간", TimeFormatter.formatDuration(60));
        assertEquals("1시간 30분", TimeFormatter.formatDuration(90));
        assertEquals("0분", TimeFormatter.formatDuration(-3));
    }

    @Test
    public void formatClock() {
        assertEquals("00:00:00", TimeFormatter.formatClock(0));
        assertEquals("00:01:05", TimeFormatter.formatClock(65_999));
        assertEquals("02:03:04", TimeFormatter.formatClock((2 * 3600 + 3 * 60 + 4) * 1000L));
        assertEquals("100:00:00", TimeFormatter.formatClock(100 * 3600 * 1000L));
    }

    @Test
    public void formatCompact() {
        assertEquals("", TimeFormatter.formatCompact(0));
        assertEquals("45m", TimeFormatter.formatCompact(45));
        assertEquals("1.5h", TimeFormatter.formatCompact(90));
        assertEquals("2h", TimeFormatter.formatCompact(120));
        assertEquals("1h", TimeFormatter.formatCompact(62));
    }
}
