package com.dayflow.ui;

import com.dayflow.model.TaskView;
import com.dayflow.store.TaskStore;
import com.dayflow.util.AppPreferences;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 设置对话框：维护 Canvas URL，通过内嵌浏览器导入作业。
 */
public class SettingsDialog extends Dialog<Integer> {

    public static Optional<Integer> show(
            TaskStore store,
            Consumer<TaskView> onImported) {
        return new SettingsDialog(store, onImported).showAndWait();
    }

    private final TaskStore store;
    private final Consumer<TaskView> onImported;

    private final TextField urlField;

    private final ButtonType importButtonType;

    private int importCount = 0;

    private SettingsDialog(TaskStore store, Consumer<TaskView> onImported) {
        this.store = store;
        this.onImported = onImported;

        setTitle("DayFlow 设置");
        setHeaderText("连接 Canvas 自动导入作业");
        setResizable(true);

        importButtonType = new ButtonType("打开内嵌浏览器导入作业",
                ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(
                importButtonType,
                new ButtonType("关闭", ButtonBar.ButtonData.CANCEL_CLOSE));

        urlField = new TextField(AppPreferences.getCanvasUrl());
        urlField.setPromptText("https://canvas.hainanu.edu.cn");

        Text tip1 = new Text("使用流程：");
        tip1.setStyle("-fx-font-weight: bold; -fx-fill: #315DB5;");
        Text tip2 = new Text("点击下方按钮 → 弹出内嵌浏览器 → 扫码/账号密码登录 Canvas"
                + " → 点浏览器顶部的蓝色“读取所有课程作业”按钮 → 自动导入并刷新任务列表。"
                + "WebView 自带 session cookie，无需手动粘贴。");
        tip2.setStyle("-fx-fill: #667085; -fx-font-size: 12px;");
        TextFlow tips = new TextFlow(tip1, tip2);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(12, 14, 12, 14));

        form.add(new Label("Canvas 地址"), 0, 0);
        form.add(urlField, 1, 0);
        form.add(tips, 0, 1, 2, 1);

        GridPane.setHgrow(urlField, javafx.scene.layout.Priority.ALWAYS);

        getDialogPane().setContent(form);
        getDialogPane().setPrefWidth(560);

        Button importBtn = (Button) getDialogPane().lookupButton(importButtonType);
        importBtn.setDefaultButton(true);
        importBtn.setOnAction(e -> {
            AppPreferences.setCanvasUrl(urlField.getText().trim());
            WebViewImportDialog.open(store, onImported);
        });

        setResultConverter(button -> button == importButtonType
                ? Integer.valueOf(importCount)
                : Integer.valueOf(0));
    }
}
