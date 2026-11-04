package com.dayflow;

import com.dayflow.model.Task;
import com.dayflow.store.TaskStore;
import com.dayflow.ui.RootView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;

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

    // 初始化演示数据（带六维度优先级）
    private static void seedDemoTasks(TaskStore store) {
        LocalDate today = LocalDate.now();

        store.add(new Task(
                "完成数据结构大作业",
                LocalTime.of(23, 59), "数据结构",
                today.plusDays(1), false, Task.Source.MANUAL,
                5, 5, 120, 4, true, 60
        ));
        store.add(new Task(
                "复习马原期中重点",
                LocalTime.of(20, 0), "马原",
                today, false, Task.Source.MANUAL,
                3, 4, 60, 2, false, 20
        ));
        store.add(new Task(
                "洗澡 + 洗衣服",
                LocalTime.of(22, 0), "生活",
                today, false, Task.Source.MANUAL,
                2, 3, 45, 1, false, 0
        ));
        store.add(new Task(
                "和朋友聚餐",
                LocalTime.of(18, 30), "生活",
                today.plusDays(2), false, Task.Source.MANUAL,
                2, 1, 120, 1, true, 0
        ));
        store.add(new Task(
                "整理项目周报",
                LocalTime.of(17, 0), "项目",
                today.plusDays(0), false, Task.Source.MANUAL,
                4, 4, 30, 2, true, 0
        ));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
