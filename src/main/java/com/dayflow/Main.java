package com.dayflow;

import com.dayflow.model.Task;
import com.dayflow.store.TaskStore;
import com.dayflow.ui.RootView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;

import javafx.scene.image.Image;

/**
 * 应用启动入口：准备数据、组装根视图并显示窗口。
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        TaskStore store = new TaskStore();
        seedDemoTasks(store);

        Scene scene = new Scene(new RootView(store), 1000, 650);
        scene.getStylesheets().add(
                getClass().getResource("/css/dayflow.css").toExternalForm()
        );

        Image icon = new Image(
                getClass().getResourceAsStream("/images/dayflow-icon.png")
        );
        stage.getIcons().add(icon);

        stage.setTitle("DayFlow - 日程管理");
        stage.setMinWidth(760);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
    }

    // 初始化演示数据
    private static void seedDemoTasks(TaskStore store) {
        LocalDate today = LocalDate.now();

        store.add(new Task(
                "完成课程作业", LocalTime.of(9, 30), "学习", today, false));
        store.add(new Task(
                "开发 DayFlow 主界面", LocalTime.of(14, 30), "项目", today, false));
        store.add(new Task(
                "复习专业课程", LocalTime.of(19, 30), "学习", today, true));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
