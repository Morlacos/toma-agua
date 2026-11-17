package com.morlacos.agua;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        if (!Prefs.running(c)) return;
        long at = Prefs.nextAt(c);
        long now = System.currentTimeMillis();
        if (at > now + 1000) Scheduler.scheduleAt(c, at);
        else Scheduler.scheduleIn(c, 60_000L);
    }
}
