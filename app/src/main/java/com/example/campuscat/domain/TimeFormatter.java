package com.example.campuscat.domain;

import java.util.Locale;

/** 화면에 보여줄 시간 문자열 포맷. */
public final class TimeFormatter {

    private TimeFormatter() {
    }

    /** 90 → "1시간 30분", 60 → "1시간", 5 → "5분". */
    public static String formatDuration(long minutes) {
        long safe = Math.max(0, minutes);
        long hours = safe / 60;
        long rest = safe % 60;
        if (hours == 0) {
            return rest + "분";
        }
        if (rest == 0) {
            return hours + "시간";
        }
        return hours + "시간 " + rest + "분";
    }

    /** 경과 시간을 "HH:MM:SS" 로. */
    public static String formatClock(long elapsedMillis) {
        long totalSeconds = Math.max(0, elapsedMillis) / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    /** 차트 라벨용 짧은 표기: 0 → "", 45 → "45m", 90 → "1.5h", 120 → "2h". */
    public static String formatCompact(long minutes) {
        if (minutes <= 0) {
            return "";
        }
        if (minutes < 60) {
            return minutes + "m";
        }
        long tenths = Math.round(minutes / 6.0);
        if (tenths % 10 == 0) {
            return (tenths / 10) + "h";
        }
        return String.format(Locale.US, "%.1fh", tenths / 10.0);
    }
}
