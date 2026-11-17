package com.morlacos.agua;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import java.util.Locale;

final class LocaleUtil {
    private LocaleUtil() {}
    static Context wrap(Context base) {
        String l = Prefs.lang(base);
        if ("auto".equals(l)) return base;
        Locale loc = Locale.forLanguageTag(l);
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        cfg.setLocales(new LocaleList(loc));
        return base.createConfigurationContext(cfg);
    }
}
