package com.dayflow.util;

import java.util.prefs.Preferences;

/**
 * 应用偏好：Canvas 地址、悬浮窗外观与全局快捷键。
 *
 * <p>存储在 JVM 的 Preferences 后端（Windows 写注册表
 * HKCU\Software\JavaSoft\Prefs）。
 */
public final class AppPreferences {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(AppPreferences.class);

    private static final String KEY_CANVAS_URL = "canvas.url";
    private static final String KEY_FLOAT_OPACITY = "float.opacity";
    private static final String KEY_FLOAT_COLOR = "float.color";
    private static final String KEY_FLOAT_WIDTH = "float.width";
    private static final String KEY_FLOAT_HEIGHT = "float.height";
    private static final String KEY_FLOAT_ROWS = "float.rows";
    private static final String KEY_HOTKEY = "hotkey";

    private static final String DEFAULT_URL = "https://canvas.hainanu.edu.cn";

    private AppPreferences() {
    }

    public static String getCanvasUrl() {
        return PREFS.get(KEY_CANVAS_URL, DEFAULT_URL);
    }

    public static void setCanvasUrl(String url) {
        PREFS.put(KEY_CANVAS_URL, url);
    }

    // —— 悬浮窗外观 ——

    public static double getFloatOpacity() {
        return PREFS.getDouble(KEY_FLOAT_OPACITY, 0.74);
    }

    public static void setFloatOpacity(double opacity) {
        PREFS.putDouble(KEY_FLOAT_OPACITY, opacity);
    }

    public static String getFloatColor() {
        return PREFS.get(KEY_FLOAT_COLOR, "#FFFFFF");
    }

    public static void setFloatColor(String hex) {
        PREFS.put(KEY_FLOAT_COLOR, hex);
    }

    public static int getFloatWidth() {
        return PREFS.getInt(KEY_FLOAT_WIDTH, 330);
    }

    public static void setFloatWidth(int width) {
        PREFS.putInt(KEY_FLOAT_WIDTH, width);
    }

    /** 窗口高度；0 表示跟随内容自动变化（用户拖拽边缘后才固定）。 */
    public static int getFloatHeight() {
        return PREFS.getInt(KEY_FLOAT_HEIGHT, 0);
    }

    public static void setFloatHeight(int height) {
        PREFS.putInt(KEY_FLOAT_HEIGHT, height);
    }

    public static int getFloatRows() {
        return PREFS.getInt(KEY_FLOAT_ROWS, 8);
    }

    public static void setFloatRows(int rows) {
        PREFS.putInt(KEY_FLOAT_ROWS, rows);
    }

    // —— 快捷键 ——

    public static String getHotkey() {
        return PREFS.get(KEY_HOTKEY, Hotkey.DEFAULT);
    }

    public static void setHotkey(String combo) {
        PREFS.put(KEY_HOTKEY, combo);
    }
}
