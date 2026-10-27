package com.example.campuscat.reminder;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.campuscat.MainActivity;
import com.example.campuscat.R;

/** 수업 알림 채널과 알림 표시. */
public final class ReminderNotifier {

    private static final String CHANNEL_ID = "class_reminder";
    private static final int NOTIFICATION_ID = 2001;

    private ReminderNotifier() {
    }

    public static void show(@NonNull Context context, @NonNull String catName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ensureChannel(context);

        Intent open = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_cat)
                .setColor(ContextCompat.getColor(context, R.color.md_primary))
                .setContentTitle(context.getString(R.string.reminder_title))
                .setContentText(context.getString(R.string.reminder_text, catName))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.reminder_channel_desc));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
