package com.dayflow.ui;

import com.dayflow.model.Task;
import com.dayflow.model.TaskView;
import com.dayflow.store.TaskStore;
import com.dayflow.util.AssignmentFilterResult;
import com.dayflow.util.AssignmentMapper;
import com.dayflow.util.Json;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 内嵌浏览器对话框：让用户扫码登录 Canvas，用同步 XHR 直接在 Java 端
 * 调用 Canvas API——WebEngine 自带 session cookie，无需鉴权 header。
 */
public class WebViewImportDialog {

    private static final String BASE = "https://canvas.hainanu.edu.cn";

    /**
     * 同步 XMLHttpRequest 版诊断脚本。
     * 所有 WebKit 版本都支持 XHR；同步模式下 executeScript 阻塞等待请求完成，
     * 直接返回结果对象，无需轮询。
     */
    private static final String JS_DIAG = """
            (function () {
                var result = { step: 'init' };
                try {
                    result.step = 'xhr open';
                    var xhr = new XMLHttpRequest();
                    xhr.open('GET', '%s/api/v1/courses?enrollment_type=student&per_page=1', false);
                    result.step = 'xhr send';
                    xhr.send(null);
                    result.step = 'xhr done status=' + xhr.status;

                    var ct = xhr.getResponseHeader('Content-Type') || '';
                    var snippet = xhr.responseText;
                    if (snippet.length > 400) snippet = snippet.substring(0, 400) + '…';

                    var cookieNames = [];
                    document.cookie.split(';').forEach(function(p) {
                        var eq = p.indexOf('=');
                        cookieNames.push(eq > 0 ? p.substring(1, eq).trim() : p.trim());
                    });

                    return JSON.stringify({
                        ok: xhr.status >= 200 && xhr.status < 300,
                        status: xhr.status,
                        contentType: ct,
                        ua: navigator.userAgent,
                        cookieCount: cookieNames.length,
                        cookieNames: cookieNames.slice(0, 15).join(', '),
                        snippet: snippet
                    });
                } catch (e) {
                    return JSON.stringify({ error: e.name + ': ' + e.message, step: result.step, ua: navigator.userAgent });
                }
            })();
            """.formatted(BASE);

    /**
     * 完整读取脚本：全部同步 XHR，跑完直接 JSON.stringify 返回。
     * 所有错误都记录到 errors 数组，不会被吞掉。
     */
    private static final String JS_READ_ALL = """
            (function () {
                function xhrGet(url) {
                    var xhr = new XMLHttpRequest();
                    xhr.open('GET', url, false);
                    try { xhr.send(null); } catch (e) { return { ok: false, error: String(e.message) }; }
                    return {
                        ok: xhr.status >= 200 && xhr.status < 300,
                        status: xhr.status,
                        contentType: xhr.getResponseHeader('Content-Type') || '',
                        body: xhr.responseText
                    };
                }

                var all = [];
                var errors = [];
                var rawCount = 0;
                var totalCourses = 0;

                try {
                    var cr = xhrGet('%s/api/v1/courses?enrollment_type=student&per_page=100');
                    if (!cr.ok) {
                        return JSON.stringify({ error: 'courses HTTP ' + cr.status, raw: 0 });
                    }
                    if (cr.contentType.indexOf('json') < 0) {
                        return JSON.stringify({ error: 'courses 响应不是 JSON（被代理拦截）',
                                contentType: cr.contentType,
                                snippet: cr.body ? cr.body.substring(0, 300) : '' });
                    }

                    var courses = JSON.parse(cr.body);
                    totalCourses = courses.length;

                    for (var i = 0; i < courses.length; i++) {
                        var c = courses[i];
                        var ar = xhrGet('%s/api/v1/courses/' + c.id
                                + '/assignments?include[]=course_name&per_page=100');
                        if (!ar.ok) {
                            errors.push({ course: c.name, id: c.id, reason: 'HTTP ' + ar.status });
                            continue;
                        }
                        if (ar.contentType.indexOf('json') < 0) {
                            errors.push({ course: c.name, id: c.id, reason: '非 JSON 响应' });
                            continue;
                        }
                        try {
                            var items = JSON.parse(ar.body);
                            rawCount += items.length;
                            for (var j = 0; j < items.length; j++) {
                                if (!items[j].course_name && c.name) items[j].course_name = c.name;
                                all.push(items[j]);
                            }
                        } catch (parseErr) {
                            errors.push({ course: c.name, id: c.id, reason: 'JSON 解析失败: ' + parseErr.message });
                        }
                    }

                    return JSON.stringify({
                        ok: true,
                        assignments: all,
                        rawCount: rawCount,
                        totalCourses: totalCourses,
                        errors: errors
                    });
                } catch (e) {
                    return JSON.stringify({ error: String(e.message || e), raw: rawCount });
                }
            })();
            """.formatted(BASE, BASE);

    // —— 对外入口 ——

    public static void open(TaskStore store, Consumer<TaskView> onImported) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("登录 Canvas 并导入作业");
        dialog.setWidth(960);
        dialog.setHeight(720);

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        TextField urlBar = new TextField(BASE);
        urlBar.setOnAction(e -> engine.load(urlBar.getText()));

        Button homeBtn = new Button("🏠");
        homeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        homeBtn.setOnAction(e -> { urlBar.setText(BASE); engine.load(BASE); });

        Button backBtn = new Button("◀");
        backBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        backBtn.setOnAction(e -> { try { engine.executeScript("history.back()"); } catch (Exception ignored) {} });

        Button reloadBtn = new Button("↻");
        reloadBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        reloadBtn.setOnAction(e -> engine.reload());

        Button diagBtn = new Button("诊断 XHR");
        diagBtn.setStyle("-fx-background-color: #7A7A7A; -fx-text-fill: white; -fx-padding: 6 12; -fx-cursor: hand;");

        Button readBtn = new Button("读取所有课程作业");
        readBtn.setStyle(
                "-fx-background-color: #315DB5; -fx-text-fill: white; -fx-font-weight: bold;"
                + "-fx-padding: 6 14; -fx-cursor: hand;");

        Label statusLabel = new Label("请登录 Canvas，登录完成后先点灰色诊断按钮确认连通性");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #315DB5; -fx-font-size: 12px; -fx-padding: 4 8;");

        // —— 诊断按钮：同步 XHR ——
        diagBtn.setOnAction(e -> {
            if (!safeLocation(engine).contains("canvas.hainanu.edu.cn")) {
                setStatus(statusLabel, "⚠ 请先在 Canvas 页面上再测", "#B54708");
                return;
            }
            diagBtn.setDisable(true);
            setStatus(statusLabel, "诊断中（同步 XHR）…", "#315DB5");

            Object raw = null;
            String jsErr = null;
            try { raw = engine.executeScript(JS_DIAG); } catch (Exception ex) { jsErr = ex.getMessage(); }

            final Object r = raw; final String je = jsErr;
            Platform.runLater(() -> {
                diagBtn.setDisable(false);
                if (je != null) { setStatus(statusLabel, "✗ JS 异常: " + je, "#E05252"); return; }
                // JS 端返回 JSON 字符串，Java 端自己 parse
                String json = r == null ? "" : String.valueOf(r);
                if (json.isBlank() || "undefined".equals(json)) {
                    setStatus(statusLabel, "✗ JS 返回空值: " + r, "#E05252");
                    return;
                }
                Map<String, Object> m;
                try { m = Json.asMap(Json.parse(json)); } catch (Exception ex) {
                    setStatus(statusLabel, "✗ JSON 解析失败: " + ex.getMessage() + "\n原始: " + r, "#E05252");
                    return;
                }
                showDiag(m, statusLabel);
            });
        });

        // —— 读取按钮：同步 XHR 跑完直接返回 JSON 字符串 ——
        readBtn.setOnAction(e -> {
            if (!safeLocation(engine).contains("canvas.hainanu.edu.cn")) {
                setStatus(statusLabel, "⚠ 当前页面不是 canvas.hainanu.edu.cn，请先登录", "#B54708");
                return;
            }

            readBtn.setDisable(true);
            setStatus(statusLabel, "读取中（18 门课 × 每门 1-2 秒，约 10-30 秒）…", "#315DB5");

            Object raw = null;
            String jsErr = null;
            try { raw = engine.executeScript(JS_READ_ALL); } catch (Exception ex) { jsErr = ex.getMessage(); }

            final Object r = raw; final String je = jsErr;
            Platform.runLater(() -> {
                readBtn.setDisable(false);
                if (je != null) { setStatus(statusLabel, "✗ JS 异常: " + je, "#E05252"); return; }

                String json = r == null ? "" : String.valueOf(r);
                Map<String, Object> m;
                try { m = Json.asMap(Json.parse(json)); } catch (Exception ex) {
                    setStatus(statusLabel, "✗ JSON 解析失败: " + ex.getMessage() + "\n原始: " + shortStr(r, 500), "#E05252");
                    return;
                }

                String err = (String) m.get("error");
                if (err != null) {
                    Object snippet = m.get("snippet");
                    setStatus(statusLabel, "✗ " + err
                            + (snippet != null ? "\n片段: " + shortStr(snippet, 300) : ""), "#E05252");
                    return;
                }

                Object listObj = m.get("assignments");
                if (!(listObj instanceof List<?> list)) {
                    setStatus(statusLabel, "✗ 无 assignments 数组: " + m, "#E05252");
                    return;
                }

                // 成功！
                int rawCount = m.get("rawCount") instanceof Number n ? n.intValue() : 0;
                Object errorsObj = m.get("errors");
                List<Map<String, Object>> errorsList = (errorsObj instanceof List<?> el)
                        ? Json.asList(el) : List.of();

                List<Map<String, Object>> assignments = Json.asList(list);
                AssignmentFilterResult filtered = AssignmentMapper.filterAndMap(assignments);
                for (Task t : filtered.tasks()) store.add(t);

                StringBuilder sb = new StringBuilder();
                sb.append("✓ 从 ").append(m.get("totalCourses")).append(" 门课拉到 ")
                        .append(rawCount).append(" 个原始作业");
                if (filtered.skippedPast() > 0) sb.append("，").append(filtered.skippedPast()).append(" 项已过期");
                if (filtered.skippedMalformed() > 0) sb.append("，").append(filtered.skippedMalformed()).append(" 项无截止日期");
                sb.append("，成功导入 ").append(filtered.tasks().size()).append(" 项");
                if (!errorsList.isEmpty()) sb.append("（").append(errorsList.size()).append(" 门课拉取失败）");

                if (filtered.tasks().isEmpty()) {
                    setStatus(statusLabel, "⚠ " + sb, "#B54708");
                } else {
                    setStatus(statusLabel, sb.toString() + "（已刷新）", "#007A33");
                    onImported.accept(TaskView.ALL);
                }

                // 列出失败的课
                if (!errorsList.isEmpty()) {
                    StringBuilder errs = new StringBuilder("以下课程拉取失败：\n");
                    int show = Math.min(errorsList.size(), 5);
                    for (int i = 0; i < show; i++) {
                        Map<String, Object> er = errorsList.get(i);
                        errs.append("• ").append(er.getOrDefault("course", "")).append(" — ")
                                .append(er.getOrDefault("reason", "")).append("\n");
                    }
                    if (errorsList.size() > 5) errs.append("…另有 ").append(errorsList.size() - 5).append(" 门");

                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.WARNING);
                    alert.setTitle("部分课程失败");
                    alert.setHeaderText("其他课程已成功导入");
                    alert.setContentText(errs.toString());
                    alert.showAndWait();
                }
            });
        });

        engine.locationProperty().addListener((o, n, newUrl) -> {
            if (newUrl != null && !newUrl.equals(urlBar.getText())) urlBar.setText(newUrl);
        });
        engine.getLoadWorker().stateProperty().addListener((o, n, s) -> {
            if (s == Worker.State.SUCCEEDED) readBtn.setDisable(false);
        });

        HBox topBar = new HBox(6, homeBtn, backBtn, reloadBtn, urlBar, diagBtn, readBtn);
        HBox.setHgrow(urlBar, Priority.ALWAYS);
        topBar.setPadding(new Insets(6, 8, 6, 8));

        VBox root = new VBox(0, topBar, webView, statusLabel);
        VBox.setVgrow(webView, Priority.ALWAYS);
        root.setStyle("-fx-background-color: #fff;");

        dialog.setScene(new Scene(root));
        dialog.show();
        engine.load(BASE);
    }

    // —— 诊断结果展示 ——

    private static void showDiag(Map<?, ?> d, Label statusLabel) {
        Object err = d.get("error");
        if (err != null) {
            Object step = d.get("step");
            Object ua = d.get("ua");
            setStatus(statusLabel,
                    "✗ XHR 异常: " + err + "（卡于 step=" + step + "）"
                    + "\nUA: " + shortStr(ua, 100), "#E05252");
            return;
        }
        Object st = d.get("status");
        Object ct = d.get("contentType");
        Object ua = d.get("ua");
        Object cc = d.get("cookieCount");
        Object cn = d.get("cookieNames");
        Object snip = d.get("snippet");

        String stStr = String.valueOf(st);
        String ctStr = String.valueOf(ct);
        String uaStr = shortStr(ua, 100);
        String snipStr = shortStr(snip, 300);
        String cookieStr = "cookie数=" + cc + " [" + (cn == null ? "" : cn) + "]";

        if ("200".equals(stStr) && ctStr.contains("json")) {
            setStatus(statusLabel,
                    "✅ XHR 通了！HTTP 200 JSON。点蓝色'读取所有课程作业'即可。"
                    + "\nUA: " + uaStr + "\n" + cookieStr, "#007A33");
        } else if ("200".equals(stStr)) {
            setStatus(statusLabel,
                    "⚠ XHR 200 但不是 JSON。Content-Type=" + ctStr
                    + "\nUA: " + uaStr + "\n" + cookieStr + "\n片段: " + snipStr, "#B54708");
        } else {
            setStatus(statusLabel,
                    "⚠ XHR HTTP " + stStr + " Content-Type=" + ctStr
                    + "\nUA: " + uaStr + "\n" + cookieStr + "\n片段: " + snipStr, "#B54708");
        }
    }

    // —— 辅助 ——

    private static String safeLocation(WebEngine engine) {
        try {
            Object loc = engine.executeScript("location.href");
            return loc == null ? "" : loc.toString();
        } catch (Exception e) { return ""; }
    }

    private static String shortStr(Object o, int max) {
        if (o == null) return "";
        String s = String.valueOf(o);
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private static void setStatus(Label label, String text, String color) {
        label.setText(text);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-padding: 4 8;");
    }
}
