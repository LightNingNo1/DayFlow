package com.dayflow.ui;

import com.dayflow.model.Task;
import com.dayflow.model.TaskPriority;
import com.dayflow.model.TaskView;
import com.dayflow.store.TaskStore;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 右侧主内容区：标题栏、任务计数和任务列表。
 *
 * <p>优先级视图下，点击卡片可展开六维雷达图和详细信息。
 */
public class TaskListView extends VBox {

    private final TaskStore store;
    private final Runnable onNewTask;
    private final Consumer<TaskView> onImported;

    private final Label pageTitle;
    private final Label countLabel;
    private final HBox header;
    private final Button addButton;
    private final Button importButton;
    private final VBox taskList = new VBox(12);

    private TaskView currentView = TaskView.TODAY;

    // 记录哪些卡片已展开（仅优先级视图生效）
    private final Map<Task, VBox> expandedDetails = new HashMap<>();

    public TaskListView(TaskStore store, Runnable onNewTask,
                        Consumer<TaskView> onImported) {
        super(22);
        this.store = store;
        this.onNewTask = onNewTask;
        this.onImported = onImported;

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

        addButton = new Button("+ 新建任务");
        addButton.setStyle(buttonBlue());
        addButton.setOnAction(e -> onNewTask.run());

        importButton = new Button("📥 从 Canvas 导入作业");
        importButton.setStyle(buttonGreen());
        importButton.setOnAction(e -> WebViewImportDialog.open(store, onImported));

        header = new HBox(12, heading, addButton);
        header.setAlignment(Pos.CENTER_LEFT);

        countLabel = new Label();
        countLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #667085;");

        ScrollPane scrollPane = new ScrollPane(taskList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(header, countLabel, scrollPane);
    }

    /** 切换视图并刷新。 */
    public void display(TaskView view) {
        currentView = view;

        pageTitle.setText(switch (view) {
            case PRIORITY -> "优先级排序";
            case TODAY -> "今天的安排";
            case HOMEWORK -> "我的作业";
            case ALL -> "所有任务";
            case COMPLETED -> "已完成的任务";
        });

        header.getChildren().remove(addButton);
        header.getChildren().remove(importButton);
        if (view == TaskView.HOMEWORK) {
            header.getChildren().add(importButton);
        } else {
            header.getChildren().add(addButton);
        }

        render();
    }

    /** 外部触发刷新（如 Canvas 导入完成后）。 */
    public void refresh() {
        render();
    }

    private void render() {
        taskList.getChildren().clear();
        expandedDetails.clear();

        List<Task> visibleTasks = store.visibleTasks(currentView);

        long unfinishedHere = visibleTasks.stream()
                .filter(task -> !task.isCompleted())
                .count();
        countLabel.setText(
                visibleTasks.size() + " 项任务 · " +
                        unfinishedHere + " 项尚未完成"
        );

        if (visibleTasks.isEmpty()) {
            String hint = switch (currentView) {
                case PRIORITY -> "还没有任何任务，先新建一项，这里会按优先级自动排序。";
                case HOMEWORK -> "这里还没有 Canvas 作业。";
                case COMPLETED -> "这里还没有已完成的任务。";
                default -> "这里还没有任务。";
            };
            Label empty = new Label(hint);
            empty.setStyle("-fx-text-fill: #98A2B3; -fx-font-size: 15px;");
            empty.setPadding(new Insets(30));
            taskList.getChildren().add(empty);
            return;
        }

        boolean expandable = (currentView == TaskView.PRIORITY);
        for (Task task : visibleTasks) {
            VBox container = createTaskRow(task, expandable);
            taskList.getChildren().add(container);
        }
    }

    /**
     * 任务行：顶部是卡片，下方可选地展开详细信息（含雷达图）。
     */
    private VBox createTaskRow(Task task, boolean expandable) {
        // —— 卡片 ——
        HBox card = buildCard(task);
        card.setCursor(expandable ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);

        // —— 展开区域（初始隐藏） ——
        VBox detail = buildDetailPanel(task);
        detail.setMaxHeight(0);
        detail.setOpacity(0);
        detail.setVisible(false);
        detail.setManaged(false);

        if (expandable) {
            card.setOnMouseClicked(e -> toggleDetail(task, detail));
        }

        VBox row = new VBox(0, card, detail);
        return row;
    }

    /** 点击卡片切换展开/收起。 */
    private void toggleDetail(Task task, VBox detail) {
        boolean show = !detail.isVisible();

        double targetH = show ? computePreferredHeight(detail) : 0;

        Timeline anim = new Timeline(new KeyFrame(
                javafx.util.Duration.millis(220),
                new KeyValue(detail.opacityProperty(), show ? 1.0 : 0.0,
                        Interpolator.EASE_BOTH)
        ));

        if (show) {
            detail.setVisible(true);
            detail.setManaged(true);
            detail.setMaxHeight(USE_PREF_SIZE);
            detail.setPrefHeight(targetH);
        } else {
            anim.setOnFinished(ev -> {
                detail.setVisible(false);
                detail.setManaged(false);
            });
        }
        anim.play();
    }

    private double computePreferredHeight(VBox detail) {
        // 临时设为显示状态测高度
        detail.setVisible(true);
        detail.setManaged(true);
        detail.setMaxHeight(USE_PREF_SIZE);
        detail.applyCss();
        detail.layout();
        double h = detail.prefHeight(-1);
        detail.setVisible(false);
        detail.setManaged(false);
        detail.setMaxHeight(0);
        return h;
    }

    // —— 构建卡片顶部 ——

    private HBox buildCard(Task task) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 18, 16, 18));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-color: #E5EAF2;" +
                        "-fx-border-radius: 12px;"
        );

        Label priorityLabel = new Label(TaskPriority.tier(task.priority()));
        priorityLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                        + TaskPriority.color(task.priority()) + ";");

        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(task.isCompleted());
        checkBox.setOnAction(e -> {
            task.setCompleted(checkBox.isSelected());
            store.touch();
            render();
        });

        VBox details = new VBox(6);

        Label title = new Label(task.getTitle());
        title.setStyle(titleStyle(task));

        Label metadata = new Label(
                task.getTime() + "   ·   " + task.getCategory()
                        + "   ·   " + task.getDate()
        );
        metadata.setStyle("-fx-font-size: 12px; -fx-text-fill: #667085;");

        details.getChildren().addAll(title, metadata);
        HBox.setHgrow(details, Priority.ALWAYS);

        Button deleteButton = new Button("删除");
        deleteButton.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #98A2B3;" +
                        "-fx-cursor: hand;");
        deleteButton.setOnAction(e -> {
            store.remove(task);
            render();
        });

        card.getChildren().addAll(priorityLabel, checkBox, details, deleteButton);
        return card;
    }

    // —— 构建展开详情面板（雷达图 + 详细信息） ——

    private VBox buildDetailPanel(Task task) {
        RadarChart radar = new RadarChart();
        radar.setValues(
                task.getImportance(), task.getUrgency(), task.getDifficulty(),
                task.getEstimatedMinutes(), task.getProgress(), task.hasTimeConstraint()
        );

        // 左侧：详细信息 + 优先级分数
        VBox info = new VBox(10);
        info.setAlignment(Pos.TOP_LEFT);
        info.setPadding(new Insets(8, 16, 8, 0));

        Label scoreLabel = new Label(String.format("优先级分数: %.2f", task.priority()));
        scoreLabel.setStyle(
                "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + TaskPriority.color(task.priority()) + ";");

        Label tierLabel = new Label("等级 " + TaskPriority.tier(task.priority()));
        tierLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #667085;");

        info.getChildren().addAll(scoreLabel, tierLabel);

        info.getChildren().add(detailLine("重要程度", task.getImportance() + "/5"));
        info.getChildren().add(detailLine("紧急程度", task.getUrgency() + "/5"));
        info.getChildren().add(detailLine("任务难度", task.getDifficulty() + "/5"));
        info.getChildren().add(detailLine("预计耗时", task.getEstimatedMinutes() + " 分钟"));
        info.getChildren().add(detailLine("完成进度", task.getProgress() + "%"));
        info.getChildren().add(detailLine("时间约束", task.hasTimeConstraint() ? "硬截止" : "无约束"));

        HBox body = new HBox(24, info, radar);
        body.setAlignment(Pos.TOP_LEFT);
        body.setPadding(new Insets(12, 20, 12, 20));

        VBox panel = new VBox(body);
        panel.setStyle(
                "-fx-background-color: #F8FAFC;" +
                        "-fx-background-radius: 0 0 12px 12px;" +
                        "-fx-border-color: #E5EAF2;" +
                        "-fx-border-width: 0 1px 1px 1px;" +
                        "-fx-border-radius: 0 0 12px 12px;"
        );
        return panel;
    }

    private HBox detailLine(String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #667085; -fx-font-size: 13px; -fx-pref-width: 80px;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: #202B3C; -fx-font-size: 13px; -fx-font-weight: 500;");
        return new HBox(8, l, v);
    }

    private String titleStyle(Task task) {
        String color;
        if (task.isCompleted()) color = "#98A2B3";
        else if (task.isOverDue()) color = "#E05252";
        else color = "#263449";
        String strike = task.isCompleted() ? "; -fx-strikethrough: true;" : "";
        return "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + color + strike;
    }

    private String buttonBlue() {
        return "-fx-background-color: #315DB5;" +
                "-fx-text-fill: white; -fx-font-size: 14px;" +
                "-fx-background-radius: 8px; -fx-padding: 11px 16px;" +
                "-fx-cursor: hand;";
    }

    private String buttonGreen() {
        return "-fx-background-color: #007A33;" +
                "-fx-text-fill: white; -fx-font-size: 14px;" +
                "-fx-background-radius: 8px; -fx-padding: 11px 16px;" +
                "-fx-cursor: hand;";
    }
}
