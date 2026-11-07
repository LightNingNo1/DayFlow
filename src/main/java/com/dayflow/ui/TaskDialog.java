package com.dayflow.ui;

import com.dayflow.model.Task;
import com.dayflow.model.TaskPriority;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * 新建任务对话框：基本信息 + 六个优先级维度（滑杆调节，实时预览优先级）。
 */
public class TaskDialog extends Dialog<Task> {

    public TaskDialog() {
        setTitle("新建任务");
        setHeaderText("安排一项新的任务");

        ButtonType saveType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        DialogTheme.apply(getDialogPane());

        // —— 基本信息 ——
        TextField titleField = new TextField();
        titleField.setPromptText("例如：完成数据结构作业");

        TextField timeField = new TextField();
        timeField.setPromptText("14:30");
        timeField.setPrefWidth(90);

        ComboBox<String> categoryBox = new ComboBox<>(
                FXCollections.observableArrayList("学习", "项目", "生活", "其他"));
        categoryBox.setValue("学习");
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        HBox timeCategory = new HBox(10, timeField, categoryBox);
        timeCategory.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(categoryBox, Priority.ALWAYS);

        GridPane basicGrid = formGrid();
        addRow(basicGrid, 0, "任务名称", titleField);
        addRow(basicGrid, 1, "时间 / 分类", timeCategory);

        VBox basicSection = section("基本信息", basicGrid);

        // —— 优先级维度 ——
        Slider importance = levelSlider();
        Slider urgency = levelSlider();
        Slider difficulty = levelSlider();

        Slider minutes = new Slider(15, 480, 60);
        minutes.setBlockIncrement(15);
        minutes.setMajorTickUnit(60);
        minutes.setMinorTickCount(3);

        Slider progress = new Slider(0, 100, 0);
        progress.setBlockIncrement(10);
        progress.setMajorTickUnit(20);
        progress.setMinorTickCount(1);

        CheckBox constraint = new CheckBox("硬截止时间（不接受延后）");
        constraint.setStyle("-fx-font-size: 12px; -fx-text-fill: #344054;");

        Label importanceValue = new Label();
        Label urgencyValue = new Label();
        Label difficultyValue = new Label();
        Label minutesValue = new Label();
        Label progressValue = new Label();

        bindValue(importance, importanceValue, v -> v + " / 5");
        bindValue(urgency, urgencyValue, v -> v + " / 5");
        bindValue(difficulty, difficultyValue, v -> v + " / 5");
        bindValue(minutes, minutesValue, v -> v + " 分钟");
        bindValue(progress, progressValue, v -> v + " %");

        Label scoreLabel = new Label();
        Label tierLabel = new Label();

        VBox dims = new VBox(10,
                sliderRow("重要程度", importance, importanceValue),
                sliderRow("紧急程度", urgency, urgencyValue),
                sliderRow("任务难度", difficulty, difficultyValue),
                sliderRow("预计耗时", minutes, minutesValue),
                sliderRow("完成进度", progress, progressValue),
                constraint
        );

        VBox scoreBox = new VBox(2, scoreLabel, tierLabel);
        scoreBox.setAlignment(Pos.CENTER_RIGHT);

        Label dimHeading = new Label("六个优先级维度");
        dimHeading.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + DialogTheme.HEADING + ";"
        );

        HBox dimHeader = new HBox(dimHeading, new VBox(), scoreBox);
        HBox.setHgrow(dimHeader.getChildren().get(1), Priority.ALWAYS);
        dimHeader.setAlignment(Pos.CENTER_LEFT);

        VBox dimSection = new VBox(12, dimHeader, dims);
        dimSection.setPadding(new Insets(14, 16, 14, 16));
        dimSection.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #E5E7EB;" +
                        "-fx-border-radius: 10;"
        );

        Runnable updateScore = () -> {
            Task probe = buildTask("预览", LocalTime.NOON, "学习",
                    importance, urgency, difficulty, minutes, progress,
                    constraint, false);
            double score = TaskPriority.score(probe);
            scoreLabel.setText(String.format("优先级 %.1f", score));
            scoreLabel.setStyle(
                    "-fx-font-size: 15px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: " + TaskPriority.color(score) + ";"
            );
            tierLabel.setText(TaskPriority.tier(score));
            tierLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #667085;");
        };

        for (Slider slider : List.of(importance, urgency, difficulty, minutes, progress)) {
            slider.valueProperty().addListener((o, ov, nv) -> updateScore.run());
        }
        constraint.selectedProperty().addListener((o, ov, nv) -> updateScore.run());
        updateScore.run();

        VBox content = new VBox(14, basicSection, dimSection);
        content.setPadding(new Insets(14, 4, 4, 4));

        getDialogPane().setContent(content);
        getDialogPane().setPrefWidth(560);

        Button saveButton = (Button) getDialogPane().lookupButton(saveType);
        DialogTheme.primary(saveButton);
        saveButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> titleField.getText().trim().isEmpty(),
                titleField.textProperty()));

        setResultConverter(button -> {
            if (button != saveType) {
                return null;
            }

            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                return null;
            }

            LocalTime time;
            String timeText = timeField.getText().trim();
            if (timeText.isEmpty()) {
                time = LocalTime.of(9, 0);
            } else {
                try {
                    time = LocalTime.parse(timeText);
                } catch (Exception e) {
                    time = LocalTime.of(9, 0);
                }
            }

            return buildTask(title, time, categoryBox.getValue(),
                    importance, urgency, difficulty, minutes, progress,
                    constraint, true);
        });
    }

    /** 从滑杆读取值拼装成任务；preview 为 true 时用于表单里的优先级预览。 */
    private static Task buildTask(String title, LocalTime time, String category,
                                  Slider importance, Slider urgency, Slider difficulty,
                                  Slider minutes, Slider progress,
                                  CheckBox constraint, boolean real) {
        return new Task(
                title,
                time,
                category,
                LocalDate.now(),
                false,
                Task.Source.MANUAL,
                (int) Math.round(importance.getValue()),
                (int) Math.round(urgency.getValue()),
                (int) Math.round(minutes.getValue()),
                (int) Math.round(difficulty.getValue()),
                constraint.isSelected(),
                (int) Math.round(progress.getValue())
        );
    }

    private static Slider levelSlider() {
        Slider slider = new Slider(1, 5, 3);
        slider.setBlockIncrement(1);
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);
        return slider;
    }

    private static void bindValue(Slider slider, Label label, IntFunction<String> format) {
        Runnable update = () -> label.setText(format.apply((int) Math.round(slider.getValue())));
        slider.valueProperty().addListener((o, ov, nv) -> update.run());
        update.run();
        label.setMinWidth(60);
        label.setStyle("-fx-text-fill: #667085; -fx-font-size: 12px;");
    }

    private static HBox sliderRow(String name, Slider slider, Label value) {
        Label label = new Label(name);
        label.setMinWidth(64);
        label.setStyle("-fx-text-fill: #344054; -fx-font-size: 12px;");

        HBox row = new HBox(10, label, slider, value);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(slider, Priority.ALWAYS);
        return row;
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

    private static VBox section(String title, Node body) {
        Label heading = new Label(title);
        heading.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + DialogTheme.HEADING + ";"
        );

        VBox box = new VBox(10, heading, body);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #E5E7EB;" +
                        "-fx-border-radius: 10;"
        );
        return box;
    }

    public static Optional<Task> showNew() {
        return new TaskDialog().showAndWait();
    }
}
