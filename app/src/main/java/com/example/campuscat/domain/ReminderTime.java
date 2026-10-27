package com.example.campuscat.domain;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** 매일 반복되는 수업 알림 시각 계산. */
public final class ReminderTime {

    private ReminderTime() {
    }

    /** {@code now} 이후 처음 돌아오는 hour:minute 시각. 지금이 정확히 그 시각이면 다음 날. */
    public static long nextTriggerMillis(long now, int hour, int minute, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(now);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return calendar.getTimeInMillis();
    }

    /** 9, 5 → "09:05". */
    public static String format(int hour, int minute) {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }
}
