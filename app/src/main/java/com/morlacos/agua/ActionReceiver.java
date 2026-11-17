package com.morlacos.agua;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ActionReceiver extends BroadcastReceiver {
    static final String DRANK = "com.morlacos.agua.DRANK";
    static final String SNOOZE = "com.morlacos.agua.SNOOZE";

    @Override public void onReceive(Context c, Intent intent) {
        String a = intent.getAction();
        Notifier.clear(c);
        if (DRANK.equals(a)) {
            Prefs.addGlass(c);
            if (Prefs.running(c)) Scheduler.scheduleIn(c, Prefs.interval(c) * 60_000L);
        } else if (SNOOZE.equals(a)) {
            if (Prefs.running(c)) Scheduler.scheduleIn(c, 5 * 60_000L);
        }
    }
}
