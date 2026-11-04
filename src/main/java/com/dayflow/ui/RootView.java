package com.dayflow.ui;

import com.dayflow.model.TaskView;
import com.dayflow.store.TaskStore;
import com.dayflow.util.AppPreferences;
import com.dayflow.util.Hotkey;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

/**
 * 根布局：左侧导航栏 + 右侧任务列表，并负责视图切换、新建任务、悬浮窗与设置。
 */
public class RootView extends BorderPane {

    private final TaskStore store;
    private final Sidebar sidebar;
    private final TaskListView taskListView;

    public RootView(TaskStore store) {
        this.store = store;

        setStyle("-fx-background-color: #F5F7F3;");

        sidebar = new Sidebar(this::show, this::toggleFloatingWindow, this::openPreferences);
        taskListView = new TaskListView(store, this::openNewTaskDialog, this::show);

        setLeft(sidebar);
        setCenter(taskListView);

        // 主窗口有焦点时用同一组合键切换穿透（全局热键由悬浮窗注册）
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (Hotkey.matches(AppPreferences.getHotkey(), event)) {
                        toggleClickThrough();
                        event.consume();
                    }
                });
            }
        });

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

    private void toggleFloatingWindow() {
        FloatingWindow.toggle(store);
    }

    private void toggleClickThrough() {
        FloatingWindow.toggleClickThrough();
    }

    private void openPreferences() {
        PreferencesDialog.show(store, this::show);
    }
}
