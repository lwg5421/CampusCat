package com.example.campuscat.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.campuscat.data.CatRepository;

/** 예약된 시각에 수업 알림을 보내고 다음 날 알림을 예약한다. */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        CatRepository repository = CatRepository.get(context);
        if (!repository.isReminderEnabled()) {
            return;
        }
        // 이미 수업 중이면 굳이 부르지 않는다.
        if (!repository.isSessionActive()) {
            ReminderNotifier.show(context, repository.getCatName());
        }
        ReminderScheduler.schedule(context);
    }
}
