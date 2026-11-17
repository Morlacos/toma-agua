package com.morlacos.agua;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        if (!Prefs.running(c)) return;
        Scheduler.scheduleIn(c, Prefs.interval(c) * 60_000L);
        Notifier.ring(c);
    }
}
