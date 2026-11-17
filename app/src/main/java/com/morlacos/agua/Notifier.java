package com.morlacos.agua;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

final class Notifier {
    private Notifier() {}
    static final String CH = "agua_alarm_v1";
    static final int ID = 42;

    static Uri alarmSound() {
        Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (u == null) u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        return u;
    }

    static AudioAttributes alarmAttrs() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
    }

    static void ensureChannel(Context base) {
        Context c = LocaleUtil.wrap(base);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel ch = nm.getNotificationChannel(CH);
        if (ch == null) {
            ch = new NotificationChannel(CH, c.getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription(c.getString(R.string.channel_desc));
            ch.setSound(alarmSound(), alarmAttrs());
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 500, 250, 500, 250, 500});
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }
    }

    static void ring(Context base) {
        Context c = LocaleUtil.wrap(base);
        ensureChannel(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        int f = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;

        Intent openI = new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(c, 10, openI, f);

        Intent fullI = new Intent(c, AlarmActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        PendingIntent full = PendingIntent.getActivity(c, 11, fullI, f);

        Notification.Builder b = new Notification.Builder(c, CH)
                .setSmallIcon(R.drawable.ic_drop)
                .setColor(c.getColor(R.color.water))
                .setContentTitle(c.getString(R.string.alarm_title))
                .setContentText(c.getString(R.string.alarm_text))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(open)
                .addAction(action(c, ActionReceiver.DRANK, R.string.drank_short, 12))
                .addAction(action(c, ActionReceiver.SNOOZE, R.string.snooze, 13));
        boolean fullOk = Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent();
        if (fullOk) b.setFullScreenIntent(full, true);
        nm.notify(ID, b.build());

        // If the app is on screen, show the big alarm screen right away.
        if (MainActivity.visible) {
            try { c.startActivity(fullI); } catch (Exception ignored) { }
        }
    }

    private static Notification.Action action(Context c, String act, int label, int req) {
        Intent i = new Intent(c, ActionReceiver.class).setAction(act);
        PendingIntent pi = PendingIntent.getBroadcast(c, req, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Action.Builder(
                Icon.createWithResource(c, R.drawable.ic_drop), c.getString(label), pi).build();
    }

    static void clear(Context c) {
        c.getSystemService(NotificationManager.class).cancel(ID);
    }
}
