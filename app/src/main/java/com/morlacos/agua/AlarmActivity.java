package com.morlacos.agua;

import android.app.Activity;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

public class AlarmActivity extends Activity {
    private Ringtone rt;
    private Vibrator vib;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override protected void attachBaseContext(Context b) { super.attachBaseContext(LocaleUtil.wrap(b)); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_alarm);

        // The notification sound stops here; this screen plays the alarm itself until you answer.
        Notifier.clear(this);
        startAlerts();

        ScaleAnimation pulse = new ScaleAnimation(1f, 1.12f, 1f, 1.12f,
                Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        pulse.setDuration(700);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        findViewById(R.id.drop).startAnimation(pulse);

        findViewById(R.id.aDone).setOnClickListener(v -> {
            Prefs.addGlass(this);
            if (Prefs.running(this)) Scheduler.scheduleIn(this, Prefs.interval(this) * 60_000L);
            finish();
        });
        findViewById(R.id.aSnooze).setOnClickListener(v -> {
            if (Prefs.running(this)) Scheduler.scheduleIn(this, 5 * 60_000L);
            finish();
        });
        h.postDelayed(this::stopAlerts, 60_000L);
    }

    private void startAlerts() {
        try {
            rt = RingtoneManager.getRingtone(this, Notifier.alarmSound());
            if (rt != null) {
                rt.setAudioAttributes(Notifier.alarmAttrs());
                if (Build.VERSION.SDK_INT >= 28) rt.setLooping(true);
                rt.play();
            }
        } catch (Exception ignored) { }
        try {
            vib = getSystemService(Vibrator.class);
            if (vib != null && vib.hasVibrator())
                vib.vibrate(VibrationEffect.createWaveform(new long[]{0, 600, 400}, 0));
        } catch (Exception ignored) { }
    }

    private void stopAlerts() {
        h.removeCallbacksAndMessages(null);
        try { if (rt != null) rt.stop(); } catch (Exception ignored) { }
        try { if (vib != null) vib.cancel(); } catch (Exception ignored) { }
    }

    @Override protected void onDestroy() {
        stopAlerts();
        super.onDestroy();
    }
}
