package com.dayflow.util;

import javafx.scene.input.KeyEvent;

import java.util.List;
import java.util.Locale;

/**
 * 快捷键解析：支持 Ctrl / Alt / Shift / Win 修饰键，加字母、数字或 F1-F12。
 *
 * <p>同一份文本同时用于 Win32 RegisterHotKey（虚拟键码 + 修饰位）和
 * JavaFX 场景内的按键匹配，避免两处配置漂移。
 */
public final class Hotkey {

    public static final String DEFAULT = "Ctrl+Alt+D";

    /** 设置面板里可选的常用组合。 */
    public static final List<String> PRESETS = List.of(
            "Ctrl+Alt+D", "Ctrl+Alt+F", "Ctrl+Alt+G", "Ctrl+Alt+T",
            "Ctrl+Shift+D", "Alt+Shift+D", "Ctrl+Alt+1", "Ctrl+Alt+2");

    // Win32 修饰位
    private static final int MOD_ALT = 0x0001;
    private static final int MOD_CONTROL = 0x0002;
    private static final int MOD_SHIFT = 0x0004;
    private static final int MOD_WIN = 0x0008;

    private Hotkey() {
    }

    /** 规范化文本，如 "ctrl + alt + d" → "Ctrl+Alt+D"。 */
    public static String normalize(String combo) {
        if (combo == null) {
            return DEFAULT;
        }
        StringBuilder sb = new StringBuilder();
        for (String raw : combo.split("\\+")) {
            String token = raw.trim();
            if (token.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('+');
            }
            sb.append(canonicalToken(token));
        }
        return sb.length() == 0 ? DEFAULT : sb.toString();
    }

    private static String canonicalToken(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "ctrl", "control" -> "Ctrl";
            case "alt" -> "Alt";
            case "shift" -> "Shift";
            case "win", "meta", "super" -> "Win";
            default -> token.length() == 1
                    ? token.toUpperCase(Locale.ROOT)
                    : token.toUpperCase(Locale.ROOT);
        };
    }

    /** 文本 → Win32 修饰位。 */
    public static int modifiers(String combo) {
        int mods = 0;
        for (String token : comboText(combo).split("\\+")) {
            switch (token.toLowerCase(Locale.ROOT)) {
                case "ctrl" -> mods |= MOD_CONTROL;
                case "alt" -> mods |= MOD_ALT;
                case "shift" -> mods |= MOD_SHIFT;
                case "win" -> mods |= MOD_WIN;
                default -> { }
            }
        }
        return mods;
    }

    /** 文本 → Win32 虚拟键码，无法识别时返回 0。 */
    public static int virtualKey(String combo) {
        String key = mainKey(combo);
        if (key.isEmpty()) {
            return 0;
        }
        if (key.length() == 1) {
            char c = key.charAt(0);
            if (c >= 'A' && c <= 'Z') {
                return 0x41 + (c - 'A');
            }
            if (c >= '0' && c <= '9') {
                return 0x30 + (c - '0');
            }
            return 0;
        }
        if (key.startsWith("F")) {
            try {
                int n = Integer.parseInt(key.substring(1));
                if (n >= 1 && n <= 24) {
                    return 0x70 + n - 1;
                }
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    /** 判断按键事件是否命中配置的组合。 */
    public static boolean matches(String combo, KeyEvent event) {
        String key = mainKey(combo);
        if (key.isEmpty() || !event.getCode().getName().equalsIgnoreCase(key)) {
            return false;
        }

        return event.isControlDown() == has(combo, "ctrl")
                && event.isAltDown() == has(combo, "alt")
                && event.isShiftDown() == has(combo, "shift")
                && event.isMetaDown() == has(combo, "win");
    }

    private static boolean has(String combo, String token) {
        for (String part : comboText(combo).split("\\+")) {
            if (part.toLowerCase(Locale.ROOT).equals(token)) {
                return true;
            }
        }
        return false;
    }

    private static String mainKey(String combo) {
        String[] parts = comboText(combo).split("\\+");
        return parts.length == 0 ? "" : parts[parts.length - 1];
    }

    private static String comboText(String combo) {
        return combo == null ? "" : normalize(combo);
    }
}
