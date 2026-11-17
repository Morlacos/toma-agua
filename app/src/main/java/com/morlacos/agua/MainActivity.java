package com.morlacos.agua;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends Activity {
    static volatile boolean visible = false;
    private static final int[] PRESETS = {15, 20, 30, 45, 60, 90, 120, 150, 180};

    private GlassView glass;
    private TextView tvTime, tvStatus, tvCount, tvGoal;
    private Button btnStart, btnQFrom, btnQTo, langEs, langEn;
    private LinearLayout chips, warnBox, drops;
    private EditText etCustom;
    private float dp;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() { refreshTimer(); h.postDelayed(this, 1000); }
    };

    @Override protected void attachBaseContext(Context b) { super.attachBaseContext(LocaleUtil.wrap(b)); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        dp = getResources().getDisplayMetrics().density;
        Notifier.ensureChannel(this);

        glass = findViewById(R.id.glass);
        tvTime = findViewById(R.id.tvTime);
        tvStatus = findViewById(R.id.tvStatus);
        tvCount = findViewById(R.id.tvCount);
        tvGoal = findViewById(R.id.tvGoal);
        btnStart = findViewById(R.id.btnStart);
        btnQFrom = findViewById(R.id.btnQFrom);
        btnQTo = findViewById(R.id.btnQTo);
        langEs = findViewById(R.id.langEs);
        langEn = findViewById(R.id.langEn);
        chips = findViewById(R.id.chips);
        warnBox = findViewById(R.id.warnBox);
        drops = findViewById(R.id.drops);
        etCustom = findViewById(R.id.etCustom);

        langEs.setOnClickListener(v -> setLang("es"));
        langEn.setOnClickListener(v -> setLang("en"));

        btnStart.setOnClickListener(v -> {
            if (Prefs.running(this)) {
                Prefs.setRunning(this, false);
                Scheduler.cancel(this);
            } else {
                askNotifications();
                Prefs.setRunning(this, true);
                Scheduler.scheduleIn(this, Prefs.interval(this) * 60_000L);
            }
            refreshAll();
        });
        findViewById(R.id.btnDrank).setOnClickListener(v -> {
            Prefs.addGlass(this);
            if (Prefs.running(this)) Scheduler.scheduleIn(this, Prefs.interval(this) * 60_000L);
            refreshAll();
        });
        findViewById(R.id.btnUndo).setOnClickListener(v -> {
            Prefs.setCount(this, Prefs.count(this) - 1);
            refreshAll();
        });
        findViewById(R.id.btnApply).setOnClickListener(v -> applyCustom());
        etCustom.setOnEditorActionListener((v, id, e) -> {
            if (id == EditorInfo.IME_ACTION_DONE) { applyCustom(); return true; }
            return false;
        });
        findViewById(R.id.goalMinus).setOnClickListener(v -> {
            Prefs.setGoal(this, Math.max(1, Prefs.goal(this) - 1)); refreshAll();
        });
        findViewById(R.id.goalPlus).setOnClickListener(v -> {
            Prefs.setGoal(this, Math.min(30, Prefs.goal(this) + 1)); refreshAll();
        });

        Switch sw = findViewById(R.id.swQuiet);
        sw.setChecked(Prefs.quiet(this));
        sw.setOnCheckedChangeListener((b, on) -> { Prefs.setQuiet(this, on); reschedule(); refreshAll(); });
        btnQFrom.setOnClickListener(v -> pickTime(true));
        btnQTo.setOnClickListener(v -> pickTime(false));

        findViewById(R.id.btnTest).setOnClickListener(v -> { askNotifications(); Notifier.ring(this); });
        findViewById(R.id.btnSound).setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                    .putExtra(Settings.EXTRA_CHANNEL_ID, Notifier.CH);
            safeStart(i);
        });
    }

    @Override protected void onResume() {
        super.onResume();
        visible = true;
        // If a reminder time passed while the app was asleep, set the next one.
        if (Prefs.running(this) && Prefs.nextAt(this) < System.currentTimeMillis() - 60_000L)
            Scheduler.scheduleIn(this, Prefs.interval(this) * 60_000L);
        refreshAll();
        h.post(ticker);
    }

    @Override protected void onPause() {
        visible = false;
        h.removeCallbacks(ticker);
        super.onPause();
    }

    private void setLang(String l) {
        Prefs.setLang(this, l);
        recreate();
    }

    private void applyCustom() {
        int m = -1;
        try { m = Integer.parseInt(etCustom.getText().toString().trim()); } catch (Exception ignored) { }
        if (m < 1 || m > 600) {
            Toast.makeText(this, R.string.invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        setInterval(m);
    }

    private void setInterval(int m) {
        Prefs.setInterval(this, m);
        reschedule();
        refreshAll();
    }

    private void reschedule() {
        if (Prefs.running(this)) Scheduler.scheduleIn(this, Prefs.interval(this) * 60_000L);
    }

    private void pickTime(boolean from) {
        int cur = from ? Prefs.quietFrom(this) : Prefs.quietTo(this);
        new TimePickerDialog(this, (v, hh, mm) -> {
            if (from) Prefs.setQuietFrom(this, hh * 60 + mm); else Prefs.setQuietTo(this, hh * 60 + mm);
            reschedule();
            refreshAll();
        }, cur / 60, cur % 60, DateFormat.is24HourFormat(this)).show();
    }

    private void askNotifications() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override public void onRequestPermissionsResult(int rc, String[] p, int[] r) {
        super.onRequestPermissionsResult(rc, p, r);
        refreshAll();
    }

    private void safeStart(Intent i) {
        try { startActivity(i); } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) { }
        }
    }

    private String fmtInterval(int m) {
        if (m < 60) return getString(R.string.min_fmt, m);
        if (m % 60 == 0) return getString(R.string.hr_fmt, m / 60);
        return getString(R.string.hrmin_fmt, m / 60, m % 60);
    }

    private String fmtClock(int minuteOfDay) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
        c.set(Calendar.MINUTE, minuteOfDay % 60);
        return DateFormat.getTimeFormat(this).format(c.getTime());
    }

    private static String mmss(long ms) {
        long s = (ms + 999) / 1000, hh = s / 3600, m = (s % 3600) / 60, x = s % 60;
        return hh > 0 ? String.format("%d:%02d:%02d", hh, m, x) : String.format("%d:%02d", m, x);
    }

    private GradientDrawable box(int fillColor, int strokeColor, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fillColor);
        g.setStroke((int) (2 * dp), strokeColor);
        g.setCornerRadius(radius * dp);
        return g;
    }

    private void refreshAll() {
        boolean running = Prefs.running(this);
        btnStart.setText(running ? R.string.stop : R.string.start);
        btnStart.setBackground(box(getColor(running ? R.color.accent : R.color.water),
                getColor(running ? R.color.accent : R.color.water), 16));
        glass.setAnimating(running);

        // language buttons
        String lang = Prefs.lang(this);
        if ("auto".equals(lang)) lang = getResources().getConfiguration().getLocales().get(0).getLanguage();
        boolean es = "es".equals(lang);
        styleToggle(langEs, es);
        styleToggle(langEn, !es);

        // interval chips, 3 per row
        chips.removeAllViews();
        int cur = Prefs.interval(this);
        LinearLayout row = null;
        for (int i = 0; i < PRESETS.length; i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
                if (i > 0) rp.topMargin = (int) (8 * dp);
                chips.addView(row, rp);
            }
            final int m = PRESETS[i];
            Button b = new Button(this);
            b.setText(fmtInterval(m));
            b.setAllCaps(false);
            b.setTextSize(15);
            b.setStateListAnimator(null);
            styleToggle(b, m == cur);
            b.setOnClickListener(v -> setInterval(m));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, (int) (48 * dp), 1f);
            if (i % 3 != 0) bp.leftMargin = (int) (8 * dp);
            row.addView(b, bp);
        }
        boolean isPreset = false;
        for (int p : PRESETS) if (p == cur) isPreset = true;
        if (!isPreset && !etCustom.hasFocus()) etCustom.setText(String.valueOf(cur));

        // today
        int n = Prefs.count(this), goal = Prefs.goal(this);
        tvCount.setText(getString(R.string.count_fmt, n, goal));
        tvGoal.setText(getString(R.string.goal_fmt, goal));
        drops.removeAllViews();
        int shownDrops = Math.min(Math.max(goal, n), 16);
        for (int i = 0; i < shownDrops; i++) {
            ImageView iv = new ImageView(this);
            iv.setImageResource(R.drawable.ic_drop);
            iv.setColorFilter(getColor(i < n ? R.color.water : R.color.line));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (28 * dp), 1f);
            drops.addView(iv, lp);
        }

        // quiet hours
        btnQFrom.setText(fmtClock(Prefs.quietFrom(this)));
        btnQTo.setText(fmtClock(Prefs.quietTo(this)));
        float a = Prefs.quiet(this) ? 1f : 0.45f;
        btnQFrom.setAlpha(a);
        btnQTo.setAlpha(a);

        refreshWarnings();
        refreshTimer();
    }

    private void styleToggle(Button b, boolean on) {
        b.setBackground(on
                ? box(getColor(R.color.water), getColor(R.color.water), 12)
                : box(getColor(R.color.card), getColor(R.color.line), 12));
        b.setTextColor(getColor(on ? R.color.white : R.color.ink));
    }

    private void refreshWarnings() {
        warnBox.removeAllViews();
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            addWarn(R.string.perm_notif, v -> {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                        || !Prefs.sp(this).getBoolean("askedNotif", false)) {
                    Prefs.sp(this).edit().putBoolean("askedNotif", true).apply();
                    askNotifications();
                } else {
                    safeStart(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
                }
            });
        }
        if (Build.VERSION.SDK_INT >= 31 && !getSystemService(AlarmManager.class).canScheduleExactAlarms()) {
            addWarn(R.string.perm_exact, v -> safeStart(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName()))));
        }
        if (Build.VERSION.SDK_INT >= 34 && !getSystemService(NotificationManager.class).canUseFullScreenIntent()) {
            addWarn(R.string.perm_fs, v -> safeStart(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:" + getPackageName()))));
        }
    }

    private void addWarn(int text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setPadding((int) (16 * dp), (int) (12 * dp), (int) (16 * dp), (int) (12 * dp));
        b.setTextColor(getColor(R.color.white));
        b.setStateListAnimator(null);
        b.setBackground(box(getColor(R.color.accent), getColor(R.color.accent), 14));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = (int) (12 * dp);
        warnBox.addView(b, lp);
    }

    private void refreshTimer() {
        long total = Prefs.interval(this) * 60_000L;
        if (!Prefs.running(this)) {
            tvTime.setText(mmss(total));
            tvStatus.setText(R.string.paused);
            glass.setLevel(1f);
            return;
        }
        long at = Prefs.nextAt(this);
        long left = Math.max(0, at - System.currentTimeMillis());
        tvTime.setText(mmss(left));
        tvStatus.setText(getString(R.string.next_at, DateFormat.getTimeFormat(this).format(new Date(at))));
        glass.setLevel(Math.min(1f, left / (float) total));
    }
}
