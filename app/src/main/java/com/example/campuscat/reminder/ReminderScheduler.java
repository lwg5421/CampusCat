package com.example.campuscat.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.example.campuscat.data.CatRepository;
import com.example.campuscat.domain.ReminderTime;

import java.util.TimeZone;

/**
 * 매일 수업 알림을 예약한다. 정확한 알람 권한이 필요 없도록 setAndAllowWhileIdle 을 쓰고,
 * 알림이 울릴 때마다 다음 날 알림을 다시 예약한다.
 */
public final class ReminderScheduler {

    private static final int REQUEST_CODE = 1001;

    private ReminderScheduler() {
    }

    /** 저장된 설정대로 알림을 켜거나 끈다. */
    public static void sync(@NonNull Context context) {
        if (CatRepository.get(context).isReminderEnabled()) {
            schedule(context);
        } else {
            cancel(context);
        }
    }

    public static void schedule(@NonNull Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }
        CatRepository repository = CatRepository.get(context);
        long triggerAt = ReminderTime.nextTriggerMillis(System.currentTimeMillis(),
                repository.getReminderHour(), repository.getReminderMinute(), TimeZone.getDefault());
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context));
    }

    public static void cancel(@NonNull Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent(context));
        }
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
