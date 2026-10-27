package com.example.campuscat.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 재부팅, 앱 업데이트, 시간대 변경 뒤에는 예약된 알람이 사라지거나 어긋나므로 다시 예약한다. */
public class RescheduleReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            ReminderScheduler.sync(context);
        }
    }
}
