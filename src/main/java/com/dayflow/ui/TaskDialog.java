package com.dayflow.ui;

import com.dayflow.model.Task;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 新建任务对话框。
 */
public class TaskDialog extends Dialog<Task> {

    public TaskDialog() {
        setTitle("新建任务");
        setHeaderText("安排一项新的任务");

        ButtonType saveButtonType = new ButtonType(
                "保存", ButtonBar.ButtonData.OK_DONE
        );

        getDialogPane().getButtonTypes().addAll(
                saveButtonType, ButtonType.CANCEL
        );

        TextField titleField = new TextField();
        titleField.setPromptText("例如：完成数据结构作业");

        TextField timeField = new TextField();
        timeField.setPromptText("例如：14:30");

        ComboBox<String> categoryBox =
                new ComboBox<>(FXCollections.observableArrayList(
                        "学习", "项目", "生活", "其他"
                ));
        categoryBox.setValue("学习");
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(14);
        form.setPadding(new Insets(20, 10, 10, 10));

        form.addRow(0, new Label("任务名称"), titleField);
        form.addRow(1, new Label("计划时间"), timeField);
        form.addRow(2, new Label("任务分类"), categoryBox);

        getDialogPane().setContent(form);
        getDialogPane().setPrefWidth(430);

        setResultConverter(button -> {
            if (button != saveButtonType) {
                return null;
            }

            String title = titleField.getText().trim();

            if (title.isEmpty()) {
                return null;
            }

            String time = timeField.getText().trim();
            if (time.isEmpty()) {
                time = "未设定时间";
            }

            return new Task(
                    title,
                    time,
                    categoryBox.getValue(),
                    LocalDate.now(),
                    false
            );
        });
    }

    /**
     * 弹出对话框并返回用户创建的任务。
     */
    public static Optional<Task> showNew() {
        return new TaskDialog().showAndWait();
    }
}
