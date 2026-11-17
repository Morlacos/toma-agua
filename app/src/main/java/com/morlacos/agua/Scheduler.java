package com.morlacos.agua;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

final class Scheduler {
    private Scheduler() {}

    static void scheduleIn(Context c, long delayMs) {
        scheduleAt(c, System.currentTimeMillis() + delayMs);
    }

    static void scheduleAt(Context c, long at) {
        at = adjustForQuiet(c, at);
        Prefs.setNextAt(c, at);
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = alarmIntent(c);
        am.cancel(pi);
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        try {
            if (exact) {
                Intent open = new Intent(c, MainActivity.class);
                PendingIntent show = PendingIntent.getActivity(c, 1, open,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show), pi);
                return;
            }
        } catch (SecurityException ignored) { }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
    }

    static void cancel(Context c) {
        c.getSystemService(AlarmManager.class).cancel(alarmIntent(c));
        Prefs.setNextAt(c, 0L);
    }

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, AlarmReceiver.class);
        return PendingIntent.getBroadcast(c, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static boolean inQuiet(Context c, int minuteOfDay) {
        if (!Prefs.quiet(c)) return false;
        int f = Prefs.quietFrom(c), t = Prefs.quietTo(c);
        if (f == t) return false;
        if (f < t) return minuteOfDay >= f && minuteOfDay < t;
        return minuteOfDay >= f || minuteOfDay < t;
    }

    /** If the time falls in quiet hours, move it to the end of the quiet period. */
    static long adjustForQuiet(Context c, long at) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(at);
        int mod = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        if (!inQuiet(c, mod)) return at;
        int t = Prefs.quietTo(c);
        cal.set(Calendar.HOUR_OF_DAY, t / 60);
        cal.set(Calendar.MINUTE, t % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= at) cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }
}
