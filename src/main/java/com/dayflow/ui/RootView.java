package com.dayflow.ui;

import com.dayflow.model.TaskView;
import com.dayflow.store.TaskStore;
import javafx.scene.layout.BorderPane;

/**
 * 根布局：左侧导航栏 + 右侧任务列表，并负责视图切换和新建任务。
 */
public class RootView extends BorderPane {

    private final TaskStore store;
    private final Sidebar sidebar;
    private final TaskListView taskListView;

    public RootView(TaskStore store) {
        this.store = store;

        setStyle("-fx-background-color: #F5F7F3;");

        sidebar = new Sidebar(this::show);
        taskListView = new TaskListView(store, this::openNewTaskDialog);

        setLeft(sidebar);
        setCenter(taskListView);

        // 默认展示“今天”
        show(TaskView.TODAY);
    }

    private void show(TaskView view) {
        sidebar.select(view);
        taskListView.display(view);
    }

    private void openNewTaskDialog() {
        TaskDialog.showNew().ifPresent(task -> {
            store.add(task);
            show(TaskView.TODAY);
        });
    }
}
