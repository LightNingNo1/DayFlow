package com.dayflow.ui;

import com.dayflow.model.Task;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * 新建任务对话框：基础信息 + 六个优先级维度。
 */
public class TaskDialog extends Dialog<Task> {

    public TaskDialog() {
        setTitle("新建任务");
        setHeaderText("安排一项新的任务");

        ButtonType saveButtonType = new ButtonType(
                "保存", ButtonBar.ButtonData.OK_DONE
        );
        getDialogPane().getButtonTypes().addAll(
                saveButtonType, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("例如：完成数据结构作业");

        TextField timeField = new TextField();
        timeField.setPromptText("例如：14:30");

        ComboBox<String> categoryBox = new ComboBox<>(
                FXCollections.observableArrayList("学习", "项目", "生活", "其他"));
        categoryBox.setValue("学习");
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        Spinner<Integer> importanceSpinner = new Spinner<>(1, 5, 3);
        Spinner<Integer> urgencySpinner = new Spinner<>(1, 5, 3);
        Spinner<Integer> difficultySpinner = new Spinner<>(1, 5, 3);
        Spinner<Integer> minutesSpinner = new Spinner<>(15, 480, 60, 15);
        Spinner<Integer> progressSpinner = new Spinner<>(0, 100, 0, 10);
        CheckBox timeConstraintBox = new CheckBox("硬截止时间（不接受延后）");

        for (Spinner<?> s : new Spinner[]{importanceSpinner, urgencySpinner,
                difficultySpinner, minutesSpinner, progressSpinner}) {
            s.setEditable(true);
            s.setMaxWidth(Double.MAX_VALUE);
        }

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.setPadding(new Insets(18, 10, 10, 10));

        form.addRow(0, new Label("任务名称"), titleField);
        form.addRow(1, new Label("计划时间"), timeField);
        form.addRow(2, new Label("任务分类"), categoryBox);

        Label dimHeader = new Label("六个优先级维度（影响排序）");
        dimHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #315DB5; -fx-font-size: 13px;");
        form.add(dimHeader, 0, 3, 2, 1);

        form.addRow(4, new Label("重要程度 1-5"), importanceSpinner);
        form.addRow(5, new Label("紧急程度 1-5"), urgencySpinner);
        form.addRow(6, new Label("任务难度 1-5"), difficultySpinner);
        form.addRow(7, new Label("预计耗时(分钟)"), minutesSpinner);
        form.addRow(8, new Label("完成进度(%)"), progressSpinner);
        form.add(timeConstraintBox, 0, 9, 2, 1);

        GridPane.setHgrow(titleField, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(timeField, javafx.scene.layout.Priority.ALWAYS);

        getDialogPane().setContent(form);
        getDialogPane().setPrefWidth(460);

        setResultConverter(button -> {
            if (button != saveButtonType) {
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
                try { time = LocalTime.parse(timeText); }
                catch (Exception e) { time = LocalTime.of(9, 0); }
            }

            return new Task(
                    title,
                    time,
                    categoryBox.getValue(),
                    LocalDate.now(),
                    false,
                    Task.Source.MANUAL,
                    importanceSpinner.getValue(),
                    urgencySpinner.getValue(),
                    minutesSpinner.getValue(),
                    difficultySpinner.getValue(),
                    timeConstraintBox.isSelected(),
                    progressSpinner.getValue()
            );
        });
    }

    public static Optional<Task> showNew() {
        return new TaskDialog().showAndWait();
    }
}
