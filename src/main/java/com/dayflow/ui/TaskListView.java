package com.dayflow.ui;

import com.dayflow.model.Task;
import com.dayflow.model.TaskView;
import com.dayflow.store.TaskStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 右侧主内容区：标题栏、任务计数和任务列表。
 */
public class TaskListView extends VBox {

    private final TaskStore store;
    private final Runnable onNewTask;

    private final Label pageTitle;
    private final Label countLabel;
    private final VBox taskList = new VBox(12);

    private TaskView currentView = TaskView.TODAY;

    public TaskListView(TaskStore store, Runnable onNewTask) {
        super(22);
        this.store = store;
        this.onNewTask = onNewTask;

        setPadding(new Insets(32, 36, 28, 36));

        pageTitle = new Label();
        pageTitle.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #202B3C;"
        );

        Label dateLabel = new Label(
                LocalDate.now().format(DateTimeFormatter.ofPattern(
                        "yyyy年M月d日 EEEE", Locale.CHINA
                ))
        );
        dateLabel.setStyle("-fx-text-fill: #667085;");

        VBox heading = new VBox(6, pageTitle, dateLabel);
        HBox.setHgrow(heading, Priority.ALWAYS);

        Button addButton = new Button("+ 新建任务");
        addButton.setStyle(
                "-fx-background-color: #315DB5;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 11px 16px;" +
                        "-fx-cursor: hand;"
        );
        addButton.setOnAction(e -> onNewTask.run());

        HBox header = new HBox(12, heading, addButton);
        header.setAlignment(Pos.CENTER_LEFT);

        countLabel = new Label();
        countLabel.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-text-fill: #667085;"
        );

        ScrollPane scrollPane = new ScrollPane(taskList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;"
        );
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(header, countLabel, scrollPane);
    }

    /**
     * 切换到指定视图并刷新列表。
     */
    public void display(TaskView view) {
        currentView = view;

        pageTitle.setText(switch (view) {
            case TODAY -> "今天的安排";
            case ALL -> "所有任务";
            case COMPLETED -> "已完成的任务";
        });

        render();
    }

    private void render() {
        taskList.getChildren().clear();

        List<Task> visibleTasks = store.visibleTasks(currentView);

        countLabel.setText(
                visibleTasks.size() + " 项任务 · " +
                        store.unfinishedCount() + " 项尚未完成"
        );

        if (visibleTasks.isEmpty()) {
            Label empty = new Label("这里还没有任务，试着新建一项吧。");
            empty.setStyle(
                    "-fx-text-fill: #98A2B3;" +
                            "-fx-font-size: 15px;"
            );
            empty.setPadding(new Insets(30));
            taskList.getChildren().add(empty);
            return;
        }

        for (Task task : visibleTasks) {
            taskList.getChildren().add(createTaskCard(task));
        }
    }

    // 创建单个任务卡片
    private HBox createTaskCard(Task task) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-color: #E5EAF2;" +
                        "-fx-border-radius: 12px;"
        );

        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(task.isCompleted());

        checkBox.setOnAction(e -> {
            task.setCompleted(checkBox.isSelected());
            render();
        });

        VBox details = new VBox(7);

        Label title = new Label(task.getTitle());
        title.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " +
                        (task.isCompleted() ? "#98A2B3" : "#263449") + ";"
        );

        if (task.isCompleted()) {
            title.setStyle(title.getStyle()
                    + "-fx-strikethrough: true;");
        }

        Label metadata = new Label(
                task.getTime() + "   ·   " + task.getCategory()
                        + "   ·   " + task.getDate()
        );
        metadata.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-text-fill: #667085;"
        );

        details.getChildren().addAll(title, metadata);
        HBox.setHgrow(details, Priority.ALWAYS);

        Button deleteButton = new Button("删除");
        deleteButton.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #98A2B3;" +
                        "-fx-cursor: hand;"
        );

        deleteButton.setOnAction(e -> {
            store.remove(task);
            render();
        });

        card.getChildren().addAll(checkBox, details, deleteButton);

        return card;
    }
}
