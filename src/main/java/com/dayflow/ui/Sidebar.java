package com.dayflow.ui;

import com.dayflow.model.TaskView;
import javafx.animation.Animation;
import javafx.animation.FillTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 左侧导航栏：Logo、视图切换按钮和底部标语。
 */
public class Sidebar extends VBox {

    // 未选中色：与高亮色同色系、透明度 0，淡出时只在 alpha 通道变化，不发灰
    private static final Color NORMAL_COLOR = Color.web("#E8EFFF", 0.0);
    private static final Color ACTIVE_COLOR = Color.web("#F8EFFF");

    // 淡入的中间色：同色系深蓝，渐变轨迹留在蓝色内，既有明显颜色流动又不发灰
    private static final Color DEEP_TINT = Color.web("#A9C2F5");

    private final Map<TaskView, NavItem> items = new EnumMap<>(TaskView.class);

    private NavItem activeItem;

    public Sidebar(Consumer<TaskView> onSelect) {
        super(18);
        setPadding(new Insets(28, 18, 20, 18));
        setPrefWidth(210);
        setStyle("-fx-background-color: white;");

        Label logo = new Label("◈  DayFlow");
        logo.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #315DB5;"
        );

        Label section = new Label("工作台");
        section.setStyle(
                "-fx-text-fill: #98A2B3;" +
                        "-fx-font-size: 12px;"
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label footer = new Label("让每一天都有条理");
        footer.setStyle("-fx-text-fill: #98A2B3;");

        getChildren().addAll(
                logo, section,
                createNavButton("☀", TaskView.TODAY, onSelect).container,
                createNavButton("☷", TaskView.ALL, onSelect).container,
                createNavButton("✓", TaskView.COMPLETED, onSelect).container,
                spacer, footer
        );
    }

    /**
     * 高亮指定导航项。
     */
    public void select(TaskView view) {
        NavItem item = items.get(view);

        if (item == null || item == activeItem) {
            return;
        }

        if (activeItem != null) {
            fadeOut(activeItem);
            activeItem.button.getStyleClass().remove("nav-button-active");
        }

        activeItem = item;

        fadeIn(activeItem);
        activeItem.button.getStyleClass().add("nav-button-active");
    }

    private NavItem createNavButton(String icon, TaskView view,
                                    Consumer<TaskView> onSelect) {
        // 1. 创建背景矩形
        Rectangle background = new Rectangle();
        background.setArcWidth(12);
        background.setArcHeight(12);
        background.setFill(NORMAL_COLOR);

        // 2. 创建按钮
        Button button = new Button(icon + "  " + view.getLabel());
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(42);
        button.getStyleClass().add("nav-button");
        button.setOnAction(e -> onSelect.accept(view));

        // 3. 使用 StackPane 叠放背景与按钮
        StackPane container = new StackPane(background, button);

        // 背景矩形随容器大小变化
        background.widthProperty().bind(container.widthProperty());
        background.heightProperty().bind(container.heightProperty());

        NavItem item = new NavItem(container, background, button);
        items.put(view, item);

        return item;
    }

    // 淡入：当前色 → 深蓝 → 浅蓝，两段渐变让选中高亮清晰可见
    private void fadeIn(NavItem item) {
        stopAnimation(item);
        Color current = (Color) item.background.getFill();

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(item.background.fillProperty(), current)),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(item.background.fillProperty(), DEEP_TINT))
        );

        item.animation = timeline;
        timeline.play();
    }

    // 淡出：当前色 → 同色透明
    private void fadeOut(NavItem item) {
        stopAnimation(item);
        Color current = (Color) item.background.getFill();

        FillTransition transition = new FillTransition(
                Duration.millis(220), item.background, current, NORMAL_COLOR);

        item.animation = transition;
        transition.play();
    }

    private void stopAnimation(NavItem item) {
        if (item.animation != null) {
            item.animation.stop();
            item.animation = null;
        }
    }

    /**
     * 一个导航项：背景矩形 + 按钮。
     */
    private static class NavItem {

        final StackPane container;
        final Rectangle background;
        final Button button;
        Animation animation;

        NavItem(StackPane container, Rectangle background, Button button) {
            this.container = container;
            this.background = background;
            this.button = button;
        }
    }
}
