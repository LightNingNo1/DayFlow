package com.dayflow.util;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Windows 原生窗口能力：按标题查找窗口句柄、切换鼠标穿透、注册全局热键。
 *
 * <p>只用 JNA 调用 user32，不引入 JNI 编译产物。非 Windows 环境下所有方法
 * 都安全降级（查找返回空、穿透与热键无效果）。
 */
public final class Win32Window {

    private static final int GWL_EXSTYLE = -20;

    private static final int WS_EX_TRANSPARENT = 0x00000020;
    private static final int WS_EX_LAYERED = 0x00080000;
    private static final int WS_EX_TOOLWINDOW = 0x00000080;
    private static final int WS_EX_NOACTIVATE = 0x08000000;

    private static final int WM_HOTKEY = 0x0312;
    private static final int PM_REMOVE = 0x0001;

    private static final int HOTKEY_ID = 0x0D1F;

    private static volatile String pendingHotkey;
    private static volatile Runnable hotkeyAction;
    private static boolean loopStarted;

    private Win32Window() {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * 在当前进程中按窗口标题查找顶层窗口句柄。
     */
    public static Optional<HWND> findWindow(String title) {
        if (!isWindows()) {
            return Optional.empty();
        }

        int pid = Kernel32.INSTANCE.GetCurrentProcessId();
        AtomicReference<HWND> found = new AtomicReference<>();

        WinUser.WNDENUMPROC callback = (hwnd, data) -> {
            if (found.get() != null) {
                return true;
            }

            IntByReference windowPid = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, windowPid);
            if (windowPid.getValue() != pid) {
                return true;
            }

            char[] text = new char[256];
            User32.INSTANCE.GetWindowText(hwnd, text, text.length);
            String windowTitle = new String(text).trim();

            if (windowTitle.equals(title)) {
                found.set(hwnd);
            }
            return true;
        };

        User32.INSTANCE.EnumWindows(callback, null);
        return Optional.ofNullable(found.get());
    }

    /**
     * 让悬浮窗出现在 Alt+Tab / 任务栏之外，像一个桌面小部件。
     */
    public static void markAsToolWindow(HWND hwnd) {
        if (!isWindows() || hwnd == null) {
            return;
        }
        int style = User32.INSTANCE.GetWindowLong(hwnd, GWL_EXSTYLE);
        User32.INSTANCE.SetWindowLong(hwnd, GWL_EXSTYLE, style | WS_EX_TOOLWINDOW);
    }

    /**
     * 开关鼠标穿透。开启后鼠标事件直接落到悬浮窗下方的窗口，
     * 悬浮窗仍然可见但完全不接受点击。
     */
    public static void setClickThrough(HWND hwnd, boolean on) {
        if (!isWindows() || hwnd == null) {
            return;
        }

        int style = User32.INSTANCE.GetWindowLong(hwnd, GWL_EXSTYLE);

        if (on) {
            style |= WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_NOACTIVATE;
        } else {
            style &= ~WS_EX_TRANSPARENT;
            style &= ~WS_EX_NOACTIVATE;
        }

        User32.INSTANCE.SetWindowLong(hwnd, GWL_EXSTYLE, style);
    }

    /**
     * 应用全局热键。可在运行期反复调用切换组合，旧组合会被自动注销。
     */
    public static void applyHotkey(String combo, Runnable action) {
        hotkeyAction = action;
        pendingHotkey = Hotkey.normalize(combo);

        if (!isWindows() || loopStarted) {
            return;
        }
        loopStarted = true;

        Thread thread = new Thread(Win32Window::hotkeyLoop, "dayflow-hotkey");
        thread.setDaemon(true);
        thread.start();
    }

    private static void hotkeyLoop() {
        String registered = null;
        WinUser.MSG msg = new WinUser.MSG();

        while (true) {
            String wanted = pendingHotkey;

            if (wanted != null && !wanted.equals(registered)) {
                if (registered != null) {
                    User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID);
                }
                int vk = Hotkey.virtualKey(wanted);
                if (vk > 0) {
                    User32.INSTANCE.RegisterHotKey(
                            null, HOTKEY_ID, Hotkey.modifiers(wanted), vk);
                }
                // 无论成功与否都记为已尝试，避免组合冲突时反复空转
                registered = wanted;
            }

            if (User32.INSTANCE.PeekMessage(msg, null, 0, 0, PM_REMOVE)) {
                if (msg.message == WM_HOTKEY) {
                    Runnable action = hotkeyAction;
                    if (action != null) {
                        action.run();
                    }
                }
            } else {
                try {
                    Thread.sleep(60);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
