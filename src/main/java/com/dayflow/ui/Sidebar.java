package com.dayflow.ui;

import com.dayflow.model.Task;
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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 左侧导航栏。
 */
public class Sidebar extends StackPane {

    private static final String ACTIVE_CLASS = "nav-button-active";
    private static final Color HIGHLIGHT_COLOR = Color.web("#bdd3fb");
    private static final Duration SLIDE_DURATION = Duration.millis(260);

    private final List<TaskView> ORDER = List.of(
            TaskView.PRIORITY, TaskView.TODAY, TaskView.HOMEWORK,
            TaskView.ALL, TaskView.COMPLETED);

    private final List<String> ICONS = List.of(
            "🎯", "☀", "📘", "☷", "✓");

    private final Map<TaskView, Button> navButtons = new EnumMap<>(TaskView.class);

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
        footer.setStyle(
                "-fx-text-fill: #98A2B3;" +
                        "-fx-font-size: 11px;"
        );

        content.getChildren().addAll(logo, section);
        for (int i = 0; i < ORDER.size(); i++) {
            content.getChildren().add(
                    createNavButton(ICONS.get(i), ORDER.get(i), onSelect));
        }
        content.getChildren().addAll(spacer, footer);

        getChildren().addAll(highlightLayer, content);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (!isSliding()) {
            updateHighlight(false);
        }
    }

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
