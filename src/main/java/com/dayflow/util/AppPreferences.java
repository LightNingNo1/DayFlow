package com.dayflow.util;

import java.util.prefs.Preferences;

/**
 * 应用偏好：Canvas URL。存储在 JVM 的 Preferences 后端
 * （Windows 写注册表 HKCU\Software\JavaSoft\Prefs）。
 */
public final class AppPreferences {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(AppPreferences.class);

    private static final String KEY_CANVAS_URL = "canvas.url";
    private static final String DEFAULT_URL = "https://canvas.hainanu.edu.cn";

    private AppPreferences() {
    }

    public static String getCanvasUrl() {
        return PREFS.get(KEY_CANVAS_URL, DEFAULT_URL);
    }

    public static void setCanvasUrl(String url) {
        PREFS.put(KEY_CANVAS_URL, url);
    }
}
