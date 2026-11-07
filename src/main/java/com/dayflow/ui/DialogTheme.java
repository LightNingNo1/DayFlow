package com.dayflow.ui;

import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 对话框统一主题：深色顶栏、浅灰底、白色圆角卡片、淡蓝主按钮。
 * 通过 data URI 样式表注入，配合少量行内样式。
 */
final class DialogTheme {

    /** 分区卡片标题颜色（深海军蓝，与主界面标题一致）。 */
    static final String HEADING = "#111827";
    /** 正文标签颜色。 */
    static final String INK = "#374151";
    /** 次要说明文字颜色。 */
    static final String MUTED = "#6B7280";

    private static final String CSS = """
            .dialog-pane {
                -fx-background-color: #F3F5F9;
            }
            .dialog-pane .header-panel {
                -fx-background-color: #1F2937;
                -fx-padding: 14 18 14 18;
            }
            .dialog-pane .header-panel .label {
                -fx-text-fill: #F9FAFB;
                -fx-font-size: 14px;
                -fx-font-weight: bold;
            }
            .dialog-pane .button-bar {
                -fx-padding: 10 14 12 14;
            }
            .text-field, .combo-box, .spinner, .color-picker {
                -fx-background-color: white;
                -fx-border-color: #D1D5DB;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
            }
            .text-field:focused, .combo-box:focused, .spinner:focused {
                -fx-border-color: #2563EB;
            }
            .slider .track {
                -fx-background-color: #E5E7EB;
            }
            .slider .thumb {
                -fx-background-color: #2563EB;
            }
            .slider .axis {
                -fx-tick-label-fill: #9CA3AF;
            }
            .check-box .box {
                -fx-background-color: white;
                -fx-border-color: #D1D5DB;
                -fx-border-radius: 4;
                -fx-background-radius: 4;
            }
            .check-box:selected .box {
                -fx-background-color: #DBEAFE;
                -fx-border-color: #2563EB;
            }
            .check-box:selected .mark {
                -fx-background-color: #2563EB;
            }
            .button {
                -fx-background-radius: 8;
                -fx-cursor: hand;
            }
            .primary-soft {
                -fx-background-color: #DBEAFE;
                -fx-text-fill: #1D4ED8;
                -fx-font-weight: bold;
                -fx-padding: 7 16 7 16;
            }
            .primary-soft:hover {
                -fx-background-color: #BFDBFE;
            }
            .primary-soft:disabled {
                -fx-opacity: 0.5;
            }
            """;

    private DialogTheme() {
    }

    /** 给对话框挂主题样式表。 */
    static void apply(DialogPane pane) {
        String uri = "data:text/css;base64,"
                + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));
        pane.getStylesheets().add(uri);
    }

    /** 把按钮标记为淡蓝主按钮（如“保存”）。 */
    static void primary(Button button) {
        button.getStyleClass().add("primary-soft");
    }
}
