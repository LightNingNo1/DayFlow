package com.dayflow.ui;

import com.dayflow.model.TaskView;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
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
 *
 * <p>选中高亮由整条侧栏共用的一个色块实现，切换视图时色块从旧按钮
 * 滑动到新按钮的位置。
 */
public class Sidebar extends StackPane {

    private static final String ACTIVE_CLASS = "nav-button-active";

    private static final Color HIGHLIGHT_COLOR = Color.web("#bdd3fb");

    private static final Duration SLIDE_DURATION = Duration.millis(260);

    private final Map<TaskView, Button> navButtons = new EnumMap<>(TaskView.class);

    // 高亮色块与它所在的透明层，层级在内容之下
    private final Rectangle highlight = new Rectangle();
    private final Pane highlightLayer = new Pane(highlight);

    private final VBox content = new VBox(18);

    private Button activeButton;
    private Timeline slide;

    public Sidebar(Consumer<TaskView> onSelect) {
        setPrefWidth(210);
        setStyle("-fx-background-color: white;");

        highlight.setArcWidth(12);
        highlight.setArcHeight(12);
        highlight.setFill(HIGHLIGHT_COLOR);
        highlight.setManaged(false);

        highlightLayer.setMouseTransparent(true);

        content.setPadding(new Insets(28, 18, 20, 18));

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

        content.getChildren().addAll(
                logo, section,
                createNavButton("☀", TaskView.TODAY, onSelect),
                createNavButton("☷", TaskView.ALL, onSelect),
                createNavButton("✓", TaskView.COMPLETED, onSelect),
                spacer, footer
        );

        getChildren().addAll(highlightLayer, content);
    }

    /**
     * 每次布局完成（首次显示、窗口缩放）都把色块对齐到当前选中项，
     * 保证应用刚打开时色块就停在“今天”上。
     */
    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        if (!isSliding()) {
            updateHighlight(false);
        }
    }

    /**
     * 高亮指定导航项，色块会从当前位置滑动过去。
     */
    public void select(TaskView view) {
        Button button = navButtons.get(view);

        if (button == null || button == activeButton) {
            return;
        }

        Button previous = activeButton;
        activeButton = button;

        if (previous != null) {
            previous.getStyleClass().remove(ACTIVE_CLASS);
        }
        button.getStyleClass().add(ACTIVE_CLASS);

        // 首次选中（还没有位置可言）时直接就位，之后都走滑动
        updateHighlight(previous != null);
    }

    private Button createNavButton(String icon, TaskView view,
                                   Consumer<TaskView> onSelect) {
        Button button = new Button(icon + "  " + view.getLabel());
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(42);
        button.getStyleClass().add("nav-button");
        button.setOnAction(e -> onSelect.accept(view));

        navButtons.put(view, button);

        return button;
    }

    /**
     * 让高亮色块对齐到当前按钮；animate 为 true 时纵向滑动过去。
     */
    private void updateHighlight(boolean animate) {
        Bounds target = activeBounds();

        if (target == null) {
            highlight.setVisible(false);
            return;
        }

        highlight.setVisible(true);

        if (slide != null) {
            slide.stop();
            slide = null;
        }

        // 横向位置与宽度直接对齐（各按钮同宽）
        highlight.setLayoutX(target.getMinX());
        highlight.setWidth(target.getWidth());

        if (!animate) {
            highlight.setLayoutY(target.getMinY());
            highlight.setHeight(target.getHeight());
            return;
        }

        slide = new Timeline(new KeyFrame(
                SLIDE_DURATION,
                new KeyValue(highlight.layoutYProperty(), target.getMinY(),
                        Interpolator.EASE_BOTH),
                new KeyValue(highlight.heightProperty(), target.getHeight(),
                        Interpolator.EASE_BOTH)
        ));

        slide.play();
    }

    /**
     * 当前选中按钮在高亮层坐标系中的位置。
     *
     * <p>直接用 VBox 已经算好的 layoutX/layoutY/width/height，而不是走
     * localToScene：首次布局时节点坐标变换尚未刷新，用变换换算会拿到
     * 上一帧（全 0）的位置，导致色块落不到“今天”上。
     */
    private Bounds activeBounds() {
        if (activeButton == null) {
            return null;
        }

        double x = content.getLayoutX() + activeButton.getLayoutX()
                - highlightLayer.getLayoutX();
        double y = content.getLayoutY() + activeButton.getLayoutY()
                - highlightLayer.getLayoutY();

        return new BoundingBox(x, y,
                activeButton.getWidth(), activeButton.getHeight());
    }

    private boolean isSliding() {
        return slide != null && slide.getStatus() == Animation.Status.RUNNING;
    }
}
