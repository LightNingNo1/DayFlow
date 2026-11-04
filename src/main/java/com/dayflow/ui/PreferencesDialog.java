package com.dayflow.ui;

import com.dayflow.model.TaskView;
import com.dayflow.store.TaskStore;
import com.dayflow.util.AppPreferences;
import com.dayflow.util.Hotkey;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * 统一设置面板：悬浮窗外观、全局快捷键、Canvas 导入。
 */
public class PreferencesDialog extends Dialog<Void> {

    public static void show(TaskStore store, Consumer<TaskView> onImported) {
        new PreferencesDialog(store, onImported).showAndWait();
    }

    private PreferencesDialog(TaskStore store, Consumer<TaskView> onImported) {
        setTitle("设置");
        setHeaderText("悬浮窗外观、快捷键与 Canvas 导入");

        ButtonType saveType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        // —— 悬浮窗外观 ——
        Slider opacity = new Slider(0.30, 1.00, AppPreferences.getFloatOpacity());
        opacity.setBlockIncrement(0.05);
        Label opacityValue = new Label(percent(opacity.getValue()));

        ColorPicker colorPicker = new ColorPicker(
                Color.web(AppPreferences.getFloatColor()));
        colorPicker.setPrefWidth(120);

        Slider width = new Slider(280, 800, Math.max(280, AppPreferences.getFloatWidth()));
        width.setBlockIncrement(10);
        Label widthValue = new Label((int) width.getValue() + " px");

        Spinner<Integer> rows = new Spinner<>(3, 12, AppPreferences.getFloatRows());
        rows.setPrefWidth(90);

        opacity.valueProperty().addListener((o, ov, nv) ->
                opacityValue.setText(percent(nv.doubleValue())));
        width.valueProperty().addListener((o, ov, nv) ->
                widthValue.setText((int) Math.round(nv.doubleValue()) + " px"));

        GridPane floatGrid = formGrid();
        addRow(floatGrid, 0, "不透明度", growRow(opacity, opacityValue));
        addRow(floatGrid, 1, "背景色", colorPicker);
        addRow(floatGrid, 2, "宽度", growRow(width, widthValue));
        addRow(floatGrid, 3, "显示条数", rows);

        VBox floatSection = section("桌面悬浮窗", floatGrid);

        // —— 快捷键 ——
        ComboBox<String> hotkey = new ComboBox<>(
                FXCollections.observableArrayList(Hotkey.PRESETS));
        hotkey.setEditable(true);
        hotkey.setValue(AppPreferences.getHotkey());
        hotkey.setMaxWidth(Double.MAX_VALUE);

        Label hotkeyHint = new Label(
                "全局生效：悬浮窗显示时切换鼠标穿透。若与系统或其他软件冲突会失效，"
                        + "此时可在主窗口内按同一组合。");
        hotkeyHint.setWrapText(true);
        hotkeyHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #98A2B3;");

        VBox hotkeyBox = new VBox(6, hotkey, hotkeyHint);
        VBox hotkeySection = section("快捷键", hotkeyBox);

        // —— Canvas ——
        TextField urlField = new TextField(AppPreferences.getCanvasUrl());
        urlField.setPromptText("https://canvas.hainanu.edu.cn");

        Button importButton = new Button("📥 从 Canvas 导入作业");
        importButton.setStyle(
                "-fx-background-color: #007A33;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 8px 14px;" +
                        "-fx-cursor: hand;"
        );
        importButton.setOnAction(e -> {
            AppPreferences.setCanvasUrl(urlField.getText().trim());
            WebViewImportDialog.open(store, onImported);
        });

        GridPane canvasGrid = formGrid();
        addRow(canvasGrid, 0, "Canvas 地址", urlField);
        HBox importRow = new HBox(importButton);
        importRow.setAlignment(Pos.CENTER_RIGHT);
        addRow(canvasGrid, 1, "", importRow);

        VBox canvasSection = section("Canvas 导入", canvasGrid);

        VBox content = new VBox(14, floatSection, hotkeySection, canvasSection);
        content.setPadding(new Insets(14, 4, 4, 4));

        getDialogPane().setContent(content);
        getDialogPane().setPrefWidth(600);

        setResultConverter(button -> {
            if (button != saveType) {
                return null;
            }

            AppPreferences.setFloatOpacity(opacity.getValue());
            AppPreferences.setFloatColor(toHex(colorPicker.getValue()));
            AppPreferences.setFloatWidth((int) Math.round(width.getValue()));
            AppPreferences.setFloatRows(rows.getValue());

            String typed = hotkey.getEditor().getText();
            String source = typed == null || typed.isBlank()
                    ? hotkey.getValue()
                    : typed;
            String combo = Hotkey.normalize(source);
            if (Hotkey.virtualKey(combo) > 0) {
                AppPreferences.setHotkey(combo);
            }

            AppPreferences.setCanvasUrl(urlField.getText().trim());
            FloatingWindow.applyPreferences();
            return null;
        });
    }

    private static String percent(double value) {
        return Math.round(value * 100) + " %";
    }

    private static String toHex(Color color) {
        return String.format(Locale.US, "#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    private static GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(84);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(labelColumn, fieldColumn);
        return grid;
    }

    private static void addRow(GridPane grid, int row, String label, Node node) {
        Label text = new Label(label);
        text.setStyle("-fx-text-fill: #344054; -fx-font-size: 12px;");
        grid.add(text, 0, row);
        grid.add(node, 1, row);
        GridPane.setHgrow(node, Priority.ALWAYS);
    }

    private static HBox growRow(Node control, Label value) {
        value.setMinWidth(60);
        value.setStyle("-fx-text-fill: #667085; -fx-font-size: 12px;");
        HBox row = new HBox(10, control, value);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(control, Priority.ALWAYS);
        return row;
    }

    private static VBox section(String title, Node body) {
        Label heading = new Label(title);
        heading.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #315DB5;"
        );

        VBox box = new VBox(10, heading, body);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle(
                "-fx-background-color: #F8FAFC;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #E5EAF2;" +
                        "-fx-border-radius: 10;"
        );
        return box;
    }
}
