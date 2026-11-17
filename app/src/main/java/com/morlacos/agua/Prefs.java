package com.morlacos.agua;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class Prefs {
    private Prefs() {}
    static SharedPreferences sp(Context c) { return c.getSharedPreferences("agua", Context.MODE_PRIVATE); }

    static String lang(Context c) { return sp(c).getString("lang", "auto"); }
    static void setLang(Context c, String v) { sp(c).edit().putString("lang", v).apply(); }

    static int interval(Context c) { return sp(c).getInt("interval", 30); }
    static void setInterval(Context c, int m) { sp(c).edit().putInt("interval", m).apply(); }

    static boolean running(Context c) { return sp(c).getBoolean("running", false); }
    static void setRunning(Context c, boolean v) { sp(c).edit().putBoolean("running", v).apply(); }

    static long nextAt(Context c) { return sp(c).getLong("nextAt", 0L); }
    static void setNextAt(Context c, long v) { sp(c).edit().putLong("nextAt", v).apply(); }

    static int goal(Context c) { return sp(c).getInt("goal", 8); }
    static void setGoal(Context c, int g) { sp(c).edit().putInt("goal", g).apply(); }

    static String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }
    static int count(Context c) {
        return today().equals(sp(c).getString("day", "")) ? sp(c).getInt("count", 0) : 0;
    }
    static void setCount(Context c, int n) {
        sp(c).edit().putString("day", today()).putInt("count", Math.max(0, n)).apply();
    }
    static void addGlass(Context c) { setCount(c, count(c) + 1); }

    static boolean quiet(Context c) { return sp(c).getBoolean("quiet", false); }
    static void setQuiet(Context c, boolean v) { sp(c).edit().putBoolean("quiet", v).apply(); }
    /** minutes after midnight */
    static int quietFrom(Context c) { return sp(c).getInt("qFrom", 22 * 60); }
    static int quietTo(Context c) { return sp(c).getInt("qTo", 7 * 60); }
    static void setQuietFrom(Context c, int m) { sp(c).edit().putInt("qFrom", m).apply(); }
    static void setQuietTo(Context c, int m) { sp(c).edit().putInt("qTo", m).apply(); }
}
